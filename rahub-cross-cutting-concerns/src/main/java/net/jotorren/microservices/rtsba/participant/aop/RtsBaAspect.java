package net.jotorren.microservices.rtsba.participant.aop;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.transaction.InvalidTransactionException;
import javax.transaction.TransactionRequiredException;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.jotorren.microservices.context.ThreadLocalContext;
import net.jotorren.microservices.rtsba.BusinessActivity;
import net.jotorren.microservices.rtsba.CoordinationContext;
import net.jotorren.microservices.rtsba.CoordinationContextParticipant;
import net.jotorren.microservices.rtsba.CreateCoordinationContext;
import net.jotorren.microservices.rtsba.RegistrationEndpoint;
import net.jotorren.microservices.rtsba.participant.RtsBaPropagation;
import net.jotorren.microservices.rtsba.participant.RtsBaTransactional;
import net.jotorren.microservices.rtsba.participant.error.RtsBaException;
import net.jotorren.microservices.rtsba.participant.error.RtsBaTimeOutException;
import net.jotorren.microservices.rtsba.protocol.BusinessActivityMessage;
import net.jotorren.microservices.rtsba.protocol.BusinessActivityMessageContentType;
import net.jotorren.microservices.rtsba.protocol.BusinessActivityMessageType;
import net.jotorren.microservices.rtsba.protocol.BusinessActivityProtocol;
import net.jotorren.microservices.rtsba.protocol.CoordinationContextMessageContentType;
import net.jotorren.microservices.rtsba.protocol.CoordinationContextMessageHeader;
import net.jotorren.microservices.rtsba.protocol.CoordinationContextProtocol;
import net.jotorren.microservices.rtsba.protocol.RtsBaMessage;

@Aspect
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class RtsBaAspect {
	private static final Logger LOG = LoggerFactory.getLogger(RtsBaAspect.class);

	@Autowired
	private CoordinationContextProtocol ctp;

	@Autowired
	private BusinessActivityProtocol bap;
	
	@Autowired
	@Qualifier("rtsbaTemplate")
	private RestTemplate restTemplate;

	@Value("${rtsba.endpoint}")
	private String rtbsEndpoint;
	
	@Value("${rtsba.activation.uri}")
	private String activationUri;

	@Value("${rtsba.status.uri}")
	private String statusUri;
	
	@Value("${rtsba.transaction.timeout}")
	private long transactionDefaultTimeout;

	@Value("${rtsba.transaction.activation.timeout}")
	private long activationDefaultTimeout;
	
	private boolean isTransactional = true;
	private boolean inherited = true;
	private boolean suspended = false;
	
    private String txid;
    private String currentContextUri;
	private long transactionTimeout;
	private long activationTimeout;
	
    private String registrationHeader;
    
    private String participantUrl;
    private List<RtsBaMessage> protocol;
    private long sequence;
    
    @Pointcut("within(@org.springframework.stereotype.Controller *)")
    public void controller() {}

    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *)")
    public void restController() {}

    @Pointcut(value = "execution(public * *(..))")
    public void anyPublicMethod() {}

    @Pointcut("execution(@org.springframework.web.bind.annotation.RequestMapping * *(..))")
    public void anyRequestMappingMethod() {}
    
    @Pointcut("execution(@org.springframework.web.bind.annotation.RequestMapping * *(net.jotorren.microservices.rtsba.protocol.BusinessActivityMessage))")
    public void callbackMethod() {}
    
    @Pointcut("execution(@net.jotorren.microservices.rtsba.participant.RtsBaTransactional * *(..))")
    public void transactionalMethod() {}

    @Around("(controller() || restController()) && anyPublicMethod() && callbackMethod()")
    public Object rtsbaProtocol(ProceedingJoinPoint point) throws Throwable {
    	Method method = ((MethodSignature) point.getSignature()).getMethod();
    	BusinessActivityMessage data = (BusinessActivityMessage)point.getArgs()[0];
    	 
        RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
        String[] consumes = requestMapping.consumes();
        RequestMethod[] httpMethod = requestMapping.method();
        boolean interceptable = (null!=consumes && consumes.length>0 && 
    			null!=httpMethod && httpMethod.length>0 && RequestMethod.PUT==httpMethod[0]);
        
    	Object result = null;
    	try {
    		ThreadLocalContext.put(RtsBaClient.RTSBA_TRANSACTION_ID, data.getCompositeTransactionId() + "-" + data.getActivityId());
    		
    		result = point.proceed();

        	if (interceptable){
        		if (BusinessActivityMessageContentType.CLOSE.equals(consumes[0])) {
        			this.bap.closed(data.getCompositeTransactionId(), data.getActivityId(), BusinessActivityMessageType.CLOSED);
        		} else if (BusinessActivityMessageContentType.COMPENSATE.equals(consumes[0])) {
            		this.bap.compensated(data.getCompositeTransactionId(), data.getActivityId(), BusinessActivityMessageType.COMPENSATED);	
        		} else if (BusinessActivityMessageContentType.CANCEL.equals(consumes[0])) {
            		this.bap.canceled(data.getCompositeTransactionId(), data.getActivityId(), BusinessActivityMessageType.CANCELED);	
        		}
        	}
        	
    	} catch (Exception e){
        	if (interceptable){
        		// TODO Test those options
        		if (BusinessActivityMessageContentType.COMPENSATE.equals(consumes[0])) {
            		this.bap.fail(data.getCompositeTransactionId(), data.getActivityId(), BusinessActivityMessageType.FAIL_COMPENSATING);	
        		} else if (BusinessActivityMessageContentType.CANCEL.equals(consumes[0])) {
            		this.bap.fail(data.getCompositeTransactionId(), data.getActivityId(), BusinessActivityMessageType.FAIL_CANCELING);	
        		}
        	}
        	
        	throw e;
    	} finally {
    		ThreadLocalContext.cleanup();
    	}
    	
    	return result;
    }
    
    @Around("(controller() || restController()) && anyPublicMethod() && anyRequestMappingMethod() && transactionalMethod()")
    public Object rtsbaParticipant(ProceedingJoinPoint point) throws Throwable {
        
        HttpServletRequest request = ((ServletRequestAttributes)RequestContextHolder.getRequestAttributes()).getRequest();
        processRequestHeaders(request);
		
        Method method = ((MethodSignature) point.getSignature()).getMethod();
        RtsBaTransactional transactional = method.getAnnotation(RtsBaTransactional.class);
        processMethodAnnotation(transactional);
        
        BusinessActivity activity = null;
        if (this.isTransactional) {
	        if (this.inherited) {
	        	LOG.info("RTS-BA AOP :: Setting a participant environment");
	        	configureAsParticipant(request, method.getAnnotation(RequestMapping.class), transactional);
		        try {
		        	activity = registerParticipant();
		        } catch (Exception e) {
			        if (transactional.strict()) {
			        	throw new RtsBaException("RTS-BA-AOP-0010", "Unable to register activity inside TX " + txid, e);
			        }
			        activity = null;
		        }
	        } else {
	        	LOG.info("RTS-BA AOP :: Setting an orquestator environment");
	        	try {
	        		activateContext();
		        } catch (Exception e) {
			        throw new RtsBaException("RTS-BA-AOP-0020", "Unable to activate context", e);
		        }        		
	        }
        }
        
        boolean activityDone = false;
        Object result = null;
        try {
        	beforeProceed();
        	result = point.proceed();
        	activityDone = true;
        	afterProceed(activity);
        } catch (RtsBaTimeOutException e) {
        	onTimeoutException(activity, activityDone, e);
        } catch (RtsBaException e) {
        	onInternalException(activity, activityDone, e);
        } catch (Exception e){
        	onBusinessException(activity, e);
        } finally {
        	ThreadLocalContext.cleanup();
        }

        return result;
    }
    
    private void beforeProceed() {
    	if (this.isTransactional && this.inherited){
    		checkContextStatus();
    	}
    }
    
    private void afterProceed(BusinessActivity activity) {
    	if (this.isTransactional){
        	checkContextStatus();
        	if (!this.inherited){
        		LOG.info("RTS-BA AOP :: Orquestator execution ends");
        		this.ctp.close(txid, ThreadLocalContext.get(RtsBaClient.RTSBA_CLIENT, RtsBaClient.class).protocol());
        	} else if (null != activity) {
        		LOG.info("RTS-BA AOP :: Participant execution ends");        		
        		this.bap.completed(txid, activity.getIdentifier(), BusinessActivityMessageType.COMPLETED);
        		this.ctp.partial(txid, ThreadLocalContext.get(RtsBaClient.RTSBA_CLIENT, RtsBaClient.class).protocol(), activity.getIdentifier());
    		}
    	} else if (this.suspended) {
			this.ctp.resume(txid, ThreadLocalContext.get(RtsBaClient.RTSBA_CLIENT, RtsBaClient.class).protocol());
			this.suspended = false;
    	}
    }
    
    private void onTimeoutException(BusinessActivity activity, boolean activityDone, RtsBaTimeOutException e){
    	if (this.isTransactional){
    		LOG.error("RTS-BA AOP :: TIMEOUT EXPIRED");
	    	if (!this.inherited){
				LOG.error("RTS-BA AOP :: Orquestator compensation");
				this.ctp.compensate(txid, ThreadLocalContext.get(RtsBaClient.RTSBA_CLIENT, RtsBaClient.class).protocol());  
	    	} if (null != activity && activityDone) {
	    		LOG.info("RTS-BA AOP :: Participant execution ends");        		
	    		this.bap.completed(txid, activity.getIdentifier(), BusinessActivityMessageType.COMPLETED);
			}
	    	throw new RtsBaException(e.getCode(), e.getMessage(), e.getCause());
    	}
    }

    private void onInternalException(BusinessActivity activity, boolean activityDone, RtsBaException e){
    	LOG.error("RTS-BA AOP :: ALREADY CAPTURED ERROR");
    	if (this.isTransactional){
	    	if (!this.inherited){
				LOG.error("RTS-BA AOP :: Orquestator compensation");
				this.ctp.compensate(txid, ThreadLocalContext.get(RtsBaClient.RTSBA_CLIENT, RtsBaClient.class).protocol());  
	    	} if (null != activity) {
	    		LOG.info("RTS-BA AOP :: Participant execution fails");
	    		this.bap.fail(txid, activity.getIdentifier(), BusinessActivityMessageType.FAIL);
			}
    	}
	    throw e;
    }
    
    private void onBusinessException(BusinessActivity activity, Exception e) throws Exception{
    	LOG.error("RTS-BA AOP :: BUSINESS ERROR");
    	if (this.isTransactional){
	    	if (!this.inherited){
				LOG.error("RTS-BA AOP :: Orquestator compensation");
				this.ctp.compensate(txid, ThreadLocalContext.get(RtsBaClient.RTSBA_CLIENT, RtsBaClient.class).protocol());  
	    	} if (null != activity) {
	    		LOG.info("RTS-BA AOP :: Participant execution fails");
	    		this.bap.fail(txid, activity.getIdentifier(), BusinessActivityMessageType.FAIL);
			}
    	}
    	throw new RtsBaException("RTS-BA-AOP-9000", "Business error", e);
    }
    
    private void processRequestHeaders(HttpServletRequest request) {
        String linkHeader = request.getHeader(CoordinationContextMessageHeader.RTSBA_CONTEXT);
        LOG.info("RTS-BA AOP :: Link header {} ", linkHeader);
		if (null != linkHeader) {
			this.currentContextUri = CoordinationContextMessageHeader.getTxContextUri(linkHeader);
		}
		LOG.info("RTS-BA AOP :: Context uri {} ", this.currentContextUri);
		
		if (null == this.currentContextUri) {
			this.inherited = false;
		} else {
			this.txid = this.currentContextUri.substring(this.currentContextUri.lastIndexOf("/") + 1);
	    	ThreadLocalContext.put(RtsBaClient.RTSBA_CONTEXT_URI, this.currentContextUri);
	    	ThreadLocalContext.put(RtsBaClient.RTSBA_TRANSACTION_ID, this.txid);
		}
		LOG.info("RTS-BA AOP :: Context identifier {} ", this.txid);
		
        this.registrationHeader = request.getHeader(CoordinationContextMessageHeader.RTSBA_REGISTER);
        LOG.info("RTS-BA AOP :: Registration header {} ", this.registrationHeader);
        
        String sequenceHeader = request.getHeader(CoordinationContextMessageHeader.RTSBA_SEQUENCE);
        LOG.info("RTS-BA AOP :: Sequence header {} ", sequenceHeader);
        this.sequence = null==sequenceHeader?1:Long.parseLong(sequenceHeader);
        LOG.info("RTS-BA AOP :: Sequence {} ", this.sequence);
		ThreadLocalContext.put(RtsBaClient.RTSBA_CLIENT, new RtsBaClient(this.sequence));
    }
    
    private void processMethodAnnotation(RtsBaTransactional transactional) {

        this.transactionTimeout = transactional.timeout()>=0?transactional.timeout():this.transactionDefaultTimeout;
        this.activationTimeout = transactional.activationTimeout()>=0?transactional.activationTimeout():this.activationDefaultTimeout;        
        checkPropagation(transactional.value());
    }
    
    private void checkContextStatus() {
		ResponseEntity<Boolean> response = 
				this.restTemplate.exchange(this.rtbsEndpoint + this.statusUri + "/" + this.txid, HttpMethod.GET, null, Boolean.class);
		if (response.getBody().booleanValue()) {
			LOG.info("RTS-BA AOP :: Context {} already closed", this.txid);
			throw new RtsBaTimeOutException("RTS-BA-AOP-30", "Context already closed", new InvalidTransactionException(this.currentContextUri));
		}
    }
    
    private void checkPropagation(RtsBaPropagation propagation) {
    	this.isTransactional = true;
    	
    	if (RtsBaPropagation.NEVER.equals(propagation)) {
    		if (null == this.currentContextUri) {
    			this.isTransactional = false;
    		} else {
    			throw new RtsBaException("RTS-BA-AOP-11", "NEVER propgation with valid TX ", new InvalidTransactionException(this.currentContextUri));
    		}
    	} else if (RtsBaPropagation.MANDATORY.equals(propagation) && null == this.currentContextUri) {
    		throw new RtsBaException("RTS-BA-AOP-12", "MANDATORY propagation with null TX ", new TransactionRequiredException());
    	} else if (RtsBaPropagation.REQUIRED.equals(propagation) && null == this.currentContextUri) {
        	this.inherited = false;
        } else if (RtsBaPropagation.REQUIRES_NEW.equals(propagation)) {
        	this.inherited = false;
        	if (null != this.currentContextUri) {
        		this.ctp.suspend(this.txid, ThreadLocalContext.get(RtsBaClient.RTSBA_CLIENT, RtsBaClient.class).protocol());
        		this.suspended = true;
        	}
        } else if (RtsBaPropagation.SUPPORTS.equals(propagation) && null == this.currentContextUri) {
        	this.isTransactional = false;
        } else if (RtsBaPropagation.NOT_SUPPORTED.equals(propagation)) {
        	this.isTransactional = false;
        	if (null != this.currentContextUri) {
        		this.ctp.suspend(this.txid, ThreadLocalContext.get(RtsBaClient.RTSBA_CLIENT, RtsBaClient.class).protocol());
        		this.suspended = true;
        	}
        }
    	
    	LOG.info("RTS-BA AOP :: Transactional " + this.isTransactional);
    	LOG.info("RTS-BA AOP :: Inherited " + this.inherited);
    	ThreadLocalContext.put(RtsBaClient.RTSBA_TRANSACTIONAL, Boolean.valueOf(this.isTransactional));
    }

    private void activateContext() throws JsonProcessingException {
		LOG.info("RTS-BA AOP :: Calling activation service provided by RTS-BA Coordinator");
		
		CreateCoordinationContext txContextRequest = new CreateCoordinationContext();
		if (null != this.currentContextUri) {
			txContextRequest.setCurrentContext(this.currentContextUri);
		}
		txContextRequest.setExpires(this.transactionTimeout);
		LOG.info("RTS-BA AOP :: Transaction timeout {} ", this.transactionTimeout);
		
		HttpHeaders headers = new HttpHeaders();
		headers.set(HttpHeaders.CONTENT_TYPE, CoordinationContextMessageContentType.RTSBA_CONTENT_TYPE);
		HttpEntity<CreateCoordinationContext> request = new HttpEntity<CreateCoordinationContext>(txContextRequest, headers);
		ResponseEntity<CoordinationContext> response = 
				this.restTemplate.exchange(this.rtbsEndpoint + this.activationUri, HttpMethod.POST, request, CoordinationContext.class);
		this.currentContextUri = response.getHeaders().getLocation().toString();
		LOG.info("RTS-BA AOP :: Context activated {}", this.currentContextUri);
		CoordinationContext context = response.getBody();
		this.txid = context.getIdentifier();
		
		ObjectMapper mapper = new ObjectMapper();
        ThreadLocalContext.put(RtsBaClient.RTSBA_CONTEXT_URI, this.currentContextUri);
        ThreadLocalContext.put(RtsBaClient.RTSBA_REGISTRATION, mapper.writeValueAsString(context.getRegistration()));
        ThreadLocalContext.put(RtsBaClient.RTSBA_CLIENT, new RtsBaClient(1));
        ThreadLocalContext.put(RtsBaClient.RTSBA_TRANSACTION_ID, this.txid);
    }
    
    private void configureAsParticipant(HttpServletRequest request, RequestMapping requestMapping, RtsBaTransactional transactional) {
    	
        String[] mapping = requestMapping.value();
        if (null == mapping || mapping.length == 0){
        	mapping = requestMapping.path();
        }
        this.participantUrl = getParticipantUrl(request.getRequestURL().toString(), mapping, transactional.path());
        LOG.info("RTS-BA AOP :: Participant url {} ", this.participantUrl);
        
        this.protocol = Arrays.asList(transactional.messages());
        if (this.protocol.contains(RtsBaMessage.NONE) ||
        		!ThreadLocalContext.get(RtsBaClient.RTSBA_TRANSACTIONAL, Boolean.class).booleanValue()){
        	// TODO Test that option
        	// An empty list of protocol messages, means NO transaction management could be done
        	this.protocol = Arrays.asList();
        	LOG.info("RTS-BA AOP :: Participant with no protocol messages");
        } else if (this.protocol.contains(RtsBaMessage.ALL)){
        	// TODO Test that option
        	this.protocol = Arrays.asList(RtsBaMessage.values());
        	this.protocol.remove(RtsBaMessage.NONE);
        	this.protocol.remove(RtsBaMessage.ALL);
        }
    }
    
    private BusinessActivity registerParticipant() throws JsonParseException, JsonMappingException, IOException {
 
		ObjectMapper mapper = new ObjectMapper();
		RegistrationEndpoint regEndpoint = mapper.readValue(registrationHeader, RegistrationEndpoint.class);
		
		LOG.info("RTS-BA AOP :: Calling registration service provided by RTS-BA Coordinator");
		CoordinationContextParticipant participant1 = new CoordinationContextParticipant();
		participant1.setAddress(this.participantUrl);
		participant1.setProtocolEvents(this.protocol);
		participant1.setActivationTimeout(this.activationTimeout);
		LOG.info("RTS-BA AOP :: Activation timeout {} ", this.activationTimeout);
		
		HttpHeaders headers = new HttpHeaders();
		headers.set(HttpHeaders.CONTENT_TYPE, CoordinationContextMessageContentType.RTSBA_REGISTER_CONTENT_TYPE);
		HttpEntity<CoordinationContextParticipant> register = 
				new HttpEntity<CoordinationContextParticipant>(participant1, headers);

		ResponseEntity<BusinessActivity> response = 
				this.restTemplate.exchange(regEndpoint.getAddress()+"/"+this.sequence, regEndpoint.getMethod(), register, BusinessActivity.class);
		BusinessActivity activity = response.getBody();
		
        ThreadLocalContext.put(RtsBaClient.RTSBA_REGISTRATION, registrationHeader);
		ThreadLocalContext.put(RtsBaClient.RTSBA_TRANSACTION_ID, this.txid + "-" + activity.getIdentifier());
		ThreadLocalContext.get(RtsBaClient.RTSBA_CLIENT, RtsBaClient.class).protocol();
		return activity;
    }

    private String getParticipantUrl(String url, String[] requestMappingPath, String transactionalPath) {
    	if (StringUtils.isEmpty(transactionalPath)) {
    		return url;
    	}
    	
    	if (transactionalPath.startsWith("http://") || transactionalPath.startsWith("https://")) {
    		return transactionalPath;
    	}
    	
    	if (null == requestMappingPath || requestMappingPath.length == 0) {
    		if (url.endsWith("/")) {
    			url = url.substring(0, url.length()-1);
    		}
    		return url + (transactionalPath.startsWith("/")?"":"/") + transactionalPath;
    	}
    	
    	int idx = url.indexOf(requestMappingPath[0]);
    	url = url.substring(0, idx);
		if (url.endsWith("/")) {
			url = url.substring(0, url.length()-1);
		}
		return url + (transactionalPath.startsWith("/")?"":"/") + transactionalPath;
    }
}
