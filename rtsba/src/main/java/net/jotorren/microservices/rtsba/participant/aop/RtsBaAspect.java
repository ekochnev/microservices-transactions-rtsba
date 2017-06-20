package net.jotorren.microservices.rtsba.participant.aop;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
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
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
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
import net.jotorren.microservices.rtsba.RtsBaProperties;
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
	private RtsBaProperties configuration;
	
	@Autowired
	private CoordinationContextProtocol ctp;

	@Autowired
	private BusinessActivityProtocol bap;
	
	@Autowired
	@Qualifier("rtsbaTemplate")
	private RestTemplate restTemplate;
	
	private boolean isTransactional = true;
	private boolean inherited = true;
	private boolean requiresNew = false;
	private boolean suspended = false;
	
	private long timeout = -1;
	
    private String currentContextUri = null;
    private String currentContextId = null;
	private String suspendedContextId = null;
	private RtsBaClient suspendedClient = null;

    private String registrationHeader = null;
    
    private String participantUrl = null;
    private List<RtsBaMessage> protocol = null;
    private long sequence = -1;
    
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
    		ThreadLocalContext.put(RtsBaClient.RTSBA_CONTEXT_ID, data.getCoordinationContextId() + "-" + data.getActivityId());
    		
    		result = point.proceed();

        	if (interceptable){
        		if (BusinessActivityMessageContentType.CLOSE.equals(consumes[0])) {
        			bap.closed(data.getCoordinationContextId(), data.getActivityId(), BusinessActivityMessageType.CLOSED);
        		} else if (BusinessActivityMessageContentType.COMPENSATE.equals(consumes[0])) {
            		bap.compensated(data.getCoordinationContextId(), data.getActivityId(), BusinessActivityMessageType.COMPENSATED);	
        		} else if (BusinessActivityMessageContentType.CANCEL.equals(consumes[0])) {
            		bap.canceled(data.getCoordinationContextId(), data.getActivityId(), BusinessActivityMessageType.CANCELED);	
        		}
        	}
        	
    	} catch (Exception e){
        	if (interceptable){
        		// TODO Test those options
        		if (BusinessActivityMessageContentType.COMPENSATE.equals(consumes[0])) {
            		bap.fail(data.getCoordinationContextId(), data.getActivityId(), BusinessActivityMessageType.FAIL_COMPENSATING);	
        		} else if (BusinessActivityMessageContentType.CANCEL.equals(consumes[0])) {
            		bap.fail(data.getCoordinationContextId(), data.getActivityId(), BusinessActivityMessageType.FAIL_CANCELING);	
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
        if (isTransactional) {
        	if (!inherited) {
	        	try {
	        		activateContext();
		        } catch (Exception e) {
			        throw new RtsBaException("RTS-BA-AOP-0020", "Unable to activate coordination context", e);
		        }        		
        	}
        	
	        if (inherited || requiresNew) {
	        	configureAsParticipant(request, method.getAnnotation(RequestMapping.class), transactional);
		        try {
		        	activity = registerParticipant();
		        } catch (Exception e) {
			        if (transactional.strict()) {
			        	throw new RtsBaException("RTS-BA-AOP-0030", "Unable to register activity inside coordination context " + currentContextId, e);
			        }
			        LOG.warn("Unable to register activity inside coordination context " + currentContextId, e);
			        activity = null;
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
    	if (isTransactional && inherited){
    		checkContextStatus();
    	}
    }
    
    private void afterProceed(BusinessActivity activity) {
    	if (isTransactional){
        	checkContextStatus();
        	if (!inherited){
        		LOG.info("RTS-BA AOP :: Orquestator execution ends");
        		if (requiresNew) {
        			ctp.partial(currentContextId, ThreadLocalContext.get(RtsBaClient.RTSBA_CLIENT, RtsBaClient.class).protocol(), activity.getIdentifier());
            	}
        		ctp.close(currentContextId, ThreadLocalContext.get(RtsBaClient.RTSBA_CLIENT, RtsBaClient.class).protocol());
        		if (requiresNew) {
        			ctp.resume(suspendedContextId, suspendedClient.protocol());
            	}
        	} else if (null != activity) {
        		LOG.info("RTS-BA AOP :: Participant execution ends");        		
        		bap.completed(currentContextId, activity.getIdentifier(), BusinessActivityMessageType.COMPLETED);
        		ctp.partial(currentContextId, ThreadLocalContext.get(RtsBaClient.RTSBA_CLIENT, RtsBaClient.class).protocol(), activity.getIdentifier());
    		}
    	} else if (suspended) {
			ctp.resume(currentContextId, ThreadLocalContext.get(RtsBaClient.RTSBA_CLIENT, RtsBaClient.class).protocol());
			suspended = false;
    	}
    }
    
    private void onTimeoutException(BusinessActivity activity, boolean activityDone, RtsBaTimeOutException e){
    	if (isTransactional){
    		LOG.error("RTS-BA AOP :: TIMEOUT EXPIRED");
	    	if (!inherited){
				LOG.error("RTS-BA AOP :: Orquestator compensation");
				ctp.compensate(currentContextId, ThreadLocalContext.get(RtsBaClient.RTSBA_CLIENT, RtsBaClient.class).protocol());
        		if (requiresNew) {
        			ctp.resume(suspendedContextId, suspendedClient.protocol());
        			return;
            	}
	    	} if (null != activity && activityDone) {
	    		LOG.info("RTS-BA AOP :: Participant execution ends");        		
	    		bap.completed(currentContextId, activity.getIdentifier(), BusinessActivityMessageType.COMPLETED);
			}
	    	throw new RtsBaException(e.getCode(), e.getMessage(), e.getCause());
    	}
    }

    private void onInternalException(BusinessActivity activity, boolean activityDone, RtsBaException e){
    	LOG.error("RTS-BA AOP :: ALREADY CAPTURED ERROR");
    	if (isTransactional){
	    	if (!inherited){
				LOG.error("RTS-BA AOP :: Orquestator compensation");
				ctp.compensate(currentContextId, ThreadLocalContext.get(RtsBaClient.RTSBA_CLIENT, RtsBaClient.class).protocol());
        		if (requiresNew) {
        			ctp.resume(suspendedContextId, suspendedClient.protocol());
        			return;
            	}
	    	} if (null != activity) {
	    		LOG.info("RTS-BA AOP :: Participant execution fails");
	    		bap.fail(currentContextId, activity.getIdentifier(), BusinessActivityMessageType.FAIL);
			}
    	}
	    throw e;
    }
    
    private void onBusinessException(BusinessActivity activity, Exception e) throws Exception{
    	LOG.error("RTS-BA AOP :: BUSINESS ERROR");
    	if (isTransactional){
	    	if (!inherited){
				LOG.error("RTS-BA AOP :: Orquestator compensation");
				ctp.compensate(currentContextId, ThreadLocalContext.get(RtsBaClient.RTSBA_CLIENT, RtsBaClient.class).protocol());
        		if (requiresNew) {
        			ctp.resume(suspendedContextId, suspendedClient.protocol());
        			return;
            	}
	    	} if (null != activity) {
	    		LOG.info("RTS-BA AOP :: Participant execution fails");
	    		bap.fail(currentContextId, activity.getIdentifier(), BusinessActivityMessageType.FAIL);
			}
    	}
    	throw new RtsBaException("RTS-BA-AOP-9000", "Business error", e);
    }
    
    private void processRequestHeaders(HttpServletRequest request) {
    	currentContextUri = null;
    	currentContextId = null;
    	
        String linkHeader = request.getHeader(CoordinationContextMessageHeader.RTSBA_CONTEXT);
        LOG.info("RTS-BA AOP :: Link header {} ", linkHeader);
		if (null != linkHeader) {
			currentContextUri = CoordinationContextMessageHeader.getTxContextUri(linkHeader);
		}
		LOG.info("RTS-BA AOP :: Coordination context uri {} ", this.currentContextUri);
		
		if (null == currentContextUri) {
			inherited = false;
		} else {
			currentContextId = currentContextUri.substring(currentContextUri.lastIndexOf("/") + 1);
	    	ThreadLocalContext.put(RtsBaClient.RTSBA_CONTEXT_URI, currentContextUri);
	    	ThreadLocalContext.put(RtsBaClient.RTSBA_CONTEXT_ID, currentContextId);
		}
		LOG.info("RTS-BA AOP :: Coordination context identifier {} ", currentContextId);
		
        registrationHeader = request.getHeader(CoordinationContextMessageHeader.RTSBA_REGISTER);
        LOG.info("RTS-BA AOP :: Registration header {} ", registrationHeader);
        
        String sequenceHeader = request.getHeader(CoordinationContextMessageHeader.RTSBA_SEQUENCE);
        LOG.info("RTS-BA AOP :: Sequence header {} ", sequenceHeader);
        sequence = null==sequenceHeader?1:Long.parseLong(sequenceHeader);
        LOG.info("RTS-BA AOP :: Sequence {} ", sequence);
		ThreadLocalContext.put(RtsBaClient.RTSBA_CLIENT, new RtsBaClient(sequence));
    }
    
    private void processMethodAnnotation(RtsBaTransactional transactional) {

        timeout = transactional.timeout()>=0?transactional.timeout():configuration.getTransactionTimeout();      
        checkPropagation(transactional.value());
    }
    
    private void checkContextStatus() {
    	LOG.info("RTS-BA AOP :: Verifying coordination context status {}", currentContextId);
		ResponseEntity<Boolean> response = 
				restTemplate.exchange(configuration.getEndpoint() + configuration.getStatusUri() + "/" + currentContextId, HttpMethod.GET, null, Boolean.class);
		if (response.getBody().booleanValue()) {
			LOG.info("RTS-BA AOP :: Coordination context {} already closed", currentContextId);
			throw new RtsBaTimeOutException("RTS-BA-AOP-40", "Context already closed", new InvalidTransactionException(currentContextUri));
		}
    }
    
    private void checkPropagation(RtsBaPropagation propagation) {
    	isTransactional = true;
    	inherited = true;
    	requiresNew = false;
    	suspended = false;
    	
    	if (RtsBaPropagation.NEVER.equals(propagation)) {
    		if (null == currentContextUri) {
    			isTransactional = false;
    		} else {
    			throw new RtsBaException("RTS-BA-AOP-10", "NEVER propagation with valid coordination context ", new InvalidTransactionException(currentContextUri));
    		}
    	} else if (RtsBaPropagation.MANDATORY.equals(propagation) && null == currentContextUri) {
    		throw new RtsBaException("RTS-BA-AOP-11", "MANDATORY propagation with null coordination context ", new TransactionRequiredException());
    	} else if (RtsBaPropagation.REQUIRED.equals(propagation) && null == currentContextUri) {
        	inherited = false;
        } else if (RtsBaPropagation.REQUIRES_NEW.equals(propagation)) {
        	inherited = false;
        	requiresNew = true;
        	if (null != currentContextUri) {
        		ctp.suspend(currentContextId, ThreadLocalContext.get(RtsBaClient.RTSBA_CLIENT, RtsBaClient.class).protocol());
        		suspended = true;
        	}
        } else if (RtsBaPropagation.SUPPORTS.equals(propagation) && null == currentContextUri) {
        	isTransactional = false;
        } else if (RtsBaPropagation.NOT_SUPPORTED.equals(propagation)) {
        	isTransactional = false;
        	if (null != currentContextUri) {
        		ctp.suspend(currentContextId, ThreadLocalContext.get(RtsBaClient.RTSBA_CLIENT, RtsBaClient.class).protocol());
        		suspended = true;
        	}
        }
    	
    	LOG.info("RTS-BA AOP :: Transactional {}", isTransactional);
    	LOG.info("RTS-BA AOP :: Inherited {}", inherited);
    	LOG.info("RTS-BA AOP :: RequiresNew {}", requiresNew);
    	ThreadLocalContext.put(RtsBaClient.RTSBA_TRANSACTIONAL, Boolean.valueOf(isTransactional));
    }

    private void activateContext() throws JsonProcessingException {
		LOG.info("RTS-BA AOP :: Calling activation service provided by RTS-BA Coordinator");

		if (requiresNew){
			suspendedClient = ThreadLocalContext.get(RtsBaClient.RTSBA_CLIENT, RtsBaClient.class);
			suspendedContextId = currentContextId;
		}
		
		CreateCoordinationContext txContextRequest = new CreateCoordinationContext();
		if (null != currentContextUri) {
			txContextRequest.setCurrentContext(currentContextUri);
		}
		txContextRequest.setExpires(timeout);
		LOG.info("RTS-BA AOP :: Transaction timeout {} ", timeout);
		
		HttpHeaders headers = new HttpHeaders();
		headers.set(HttpHeaders.CONTENT_TYPE, CoordinationContextMessageContentType.RTSBA_CONTENT_TYPE);
		HttpEntity<CreateCoordinationContext> request = new HttpEntity<CreateCoordinationContext>(txContextRequest, headers);
		ResponseEntity<CoordinationContext> response = 
				restTemplate.exchange(configuration.getEndpoint() + configuration.getActivationUri(), HttpMethod.POST, request, CoordinationContext.class);
		currentContextUri = response.getHeaders().getLocation().toString();
		LOG.info("RTS-BA AOP :: Context activated {}", currentContextUri);
		
		CoordinationContext context = response.getBody();
		currentContextId = context.getIdentifier();
	
		ObjectMapper mapper = new ObjectMapper();
		registrationHeader = mapper.writeValueAsString(context.getRegistration());
			
        ThreadLocalContext.put(RtsBaClient.RTSBA_CONTEXT_URI, currentContextUri);
        ThreadLocalContext.put(RtsBaClient.RTSBA_REGISTRATION, registrationHeader);
        ThreadLocalContext.put(RtsBaClient.RTSBA_CLIENT, new RtsBaClient(1));
        ThreadLocalContext.put(RtsBaClient.RTSBA_CONTEXT_ID, currentContextId);
    }
    
    private void configureAsParticipant(HttpServletRequest request, RequestMapping requestMapping, RtsBaTransactional transactional) {
    	
        String[] mapping = requestMapping.value();
        if (null == mapping || mapping.length == 0){
        	mapping = requestMapping.path();
        }
        participantUrl = getParticipantUrl(request.getRequestURL().toString(), mapping, transactional.path());
        LOG.info("RTS-BA AOP :: Participant url {} ", participantUrl);
        
        protocol = Arrays.asList(transactional.messages());
        if (protocol.contains(RtsBaMessage.NONE) ||
        		!ThreadLocalContext.get(RtsBaClient.RTSBA_TRANSACTIONAL, Boolean.class).booleanValue()){
        	// TODO Test that option
        	// An empty list of protocol messages, means NO transaction management could be done
        	protocol = new ArrayList<>();
        	LOG.info("RTS-BA AOP :: Participant with no protocol messages");
        } else if (protocol.contains(RtsBaMessage.ALL)){
        	// TODO Test that option
        	protocol = new ArrayList<>();
        	protocol.add(RtsBaMessage.CANCEL);
        	protocol.add(RtsBaMessage.CLOSE);
        	protocol.add(RtsBaMessage.COMPENSATE);
        	protocol.add(RtsBaMessage.EXITED);
        	protocol.add(RtsBaMessage.FAILED);
        	protocol.add(RtsBaMessage.NOT_COMPLETED);
        }
    }
    
    private BusinessActivity registerParticipant() throws JsonParseException, JsonMappingException, IOException {
 
		ObjectMapper mapper = new ObjectMapper();
		RegistrationEndpoint regEndpoint = mapper.readValue(registrationHeader, RegistrationEndpoint.class);
		
		LOG.info("RTS-BA AOP :: Calling registration service provided by RTS-BA Coordinator");
		CoordinationContextParticipant participant1 = new CoordinationContextParticipant();
		participant1.setAddress(participantUrl);
		participant1.setProtocolEvents(protocol);
		
		HttpHeaders headers = new HttpHeaders();
		headers.set(HttpHeaders.CONTENT_TYPE, CoordinationContextMessageContentType.RTSBA_REGISTER_CONTENT_TYPE);
		HttpEntity<CoordinationContextParticipant> register = 
				new HttpEntity<CoordinationContextParticipant>(participant1, headers);

		ResponseEntity<BusinessActivity> response = 
				restTemplate.exchange(regEndpoint.getAddress()+"/"+sequence, regEndpoint.getMethod(), register, BusinessActivity.class);
		BusinessActivity activity = response.getBody();
		
        ThreadLocalContext.put(RtsBaClient.RTSBA_REGISTRATION, registrationHeader);
		ThreadLocalContext.put(RtsBaClient.RTSBA_CONTEXT_ID, currentContextId + "-" + activity.getIdentifier());
		ThreadLocalContext.get(RtsBaClient.RTSBA_CLIENT, RtsBaClient.class).protocol();
		return activity;
    }

    private String getParticipantUrl(String url, String[] requestMappingPath, String transactionalPath) {
    	if (null == transactionalPath){
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
