package net.jotorren.microservices.rtsba.coordinator.saga;

import java.util.HashMap;
import java.util.Map;

import org.axonframework.eventhandling.saga.EndSaga;
import org.axonframework.eventhandling.saga.SagaEventHandler;
import org.axonframework.eventhandling.saga.StartSaga;
import org.axonframework.spring.stereotype.Saga;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import net.jotorren.microservices.rtsba.CoordinationContextParticipant;
import net.jotorren.microservices.rtsba.protocol.BusinessActivityMessage;
import net.jotorren.microservices.rtsba.protocol.BusinessActivityMessageContentType;
import net.jotorren.microservices.rtsba.protocol.BusinessActivityMessageType;
import net.jotorren.microservices.rtsba.protocol.BusinessActivityProtocol;
import net.jotorren.microservices.rtsba.protocol.BusinessActivityStatus;
import net.jotorren.microservices.rtsba.protocol.RtsBaMessage;
import net.jotorren.microservices.rtsba.protocol.event.BusinessActivityActivationEvent;
import net.jotorren.microservices.rtsba.protocol.event.BusinessActivityCancelEvent;
import net.jotorren.microservices.rtsba.protocol.event.BusinessActivityCanceledEvent;
import net.jotorren.microservices.rtsba.protocol.event.BusinessActivityCannotCompleteEvent;
import net.jotorren.microservices.rtsba.protocol.event.BusinessActivityCloseEvent;
import net.jotorren.microservices.rtsba.protocol.event.BusinessActivityClosedEvent;
import net.jotorren.microservices.rtsba.protocol.event.BusinessActivityCompensateEvent;
import net.jotorren.microservices.rtsba.protocol.event.BusinessActivityCompensatedEvent;
import net.jotorren.microservices.rtsba.protocol.event.BusinessActivityCompletedEvent;
import net.jotorren.microservices.rtsba.protocol.event.BusinessActivityExitEvent;
import net.jotorren.microservices.rtsba.protocol.event.BusinessActivityExitedEvent;
import net.jotorren.microservices.rtsba.protocol.event.BusinessActivityFailCancelingEvent;
import net.jotorren.microservices.rtsba.protocol.event.BusinessActivityFailCompensatingEvent;
import net.jotorren.microservices.rtsba.protocol.event.BusinessActivityFailEvent;
import net.jotorren.microservices.rtsba.protocol.event.BusinessActivityFailedEvent;
import net.jotorren.microservices.rtsba.protocol.event.BusinessActivityNotCompletedEvent;

// TODO When processing an event we must check if the current state allows the given transition
@Saga
public class BusinessActivitySaga {

	public transient static final String ASSOCIATION_PROPERTY = "activityInstanceId";
	
	private transient static final Logger LOG = LoggerFactory.getLogger(BusinessActivitySaga.class);

	@Autowired
	private transient StringRedisTemplate redis;

	@Autowired
	@Qualifier("rtsbaTemplate")
	private transient RestTemplate restTemplate;

	@Autowired
	private transient BusinessActivityProtocol bap;
	
	private String coordCtxId;
	private String activityId;
	private BusinessActivityStatus status;
	private CoordinationContextParticipant participant;
	
	private void send(String contentType) {
		HttpHeaders headers = new HttpHeaders();
		headers.set(HttpHeaders.CONTENT_TYPE, contentType);

	    Map<String, String> param = new HashMap<String, String>();
	    
		BusinessActivityMessage data = new BusinessActivityMessage(coordCtxId, activityId, status);
		
		HttpEntity<BusinessActivityMessage> request = new HttpEntity<BusinessActivityMessage>(data, headers);
		restTemplate.exchange(participant.getAddress(), HttpMethod.PUT, request, Void.class, param);	
	}
	
	@SagaEventHandler(associationProperty = ASSOCIATION_PROPERTY)
	@StartSaga
	public void handle(BusinessActivityActivationEvent event) {
		coordCtxId = event.getCoordinationContextId();
		activityId = event.getActivityId();
		participant = event.getParticipant();
		status = BusinessActivityStatus.ACTIVE;
		
		redis.convertAndSend(coordCtxId + "-" + activityId, BusinessActivityStatus.ACTIVE.toString());
		LOG.info("RTS-BA SAGA-ACTV :: Coordination context {} :: Activity {} << ACTIVE", coordCtxId, activityId);
	}

	@SagaEventHandler(associationProperty = ASSOCIATION_PROPERTY)
	public void handle(BusinessActivityExitEvent event) {
		status = BusinessActivityStatus.EXITING;
		LOG.info("RTS-BA SAGA-ACTV :: Coordination context {} :: Activity {} >> EXIT", coordCtxId, activityId);

		if (participant.getProtocolEvents().contains(RtsBaMessage.EXITED)){
			send(BusinessActivityMessageContentType.EXITED);
		}
		
		// we must generate a EXITED event in order to end the business activity saga
		bap.exited(coordCtxId, activityId, BusinessActivityMessageType.EXITED);	
	}

	@SagaEventHandler(associationProperty = ASSOCIATION_PROPERTY)
	public void handle(BusinessActivityCannotCompleteEvent event) {
		status = BusinessActivityStatus.NOTCOMPLETING;
		LOG.info("RTS-BA SAGA-ACTV :: Coordination context {} :: Activity {} >> CANNOT COMPLETE", coordCtxId, activityId);
		
		if (participant.getProtocolEvents().contains(RtsBaMessage.NOT_COMPLETED)){
			send(BusinessActivityMessageContentType.NOT_COMPLETED);
		}
		
		// we must generate a NOT COMPLETED event in order to end the business activity saga
		bap.notCompleted(coordCtxId, activityId, BusinessActivityMessageType.NOT_COMPLETED);	
	}
	
	@SagaEventHandler(associationProperty = ASSOCIATION_PROPERTY)
	public void handle(BusinessActivityCompletedEvent event) {
		status = BusinessActivityStatus.COMPLETED;
		LOG.info("RTS-BA SAGA-ACTV :: Coordination context {} :: Activity {} >> COMPLETED", coordCtxId, activityId);
	}

	@SagaEventHandler(associationProperty = ASSOCIATION_PROPERTY)
	public void handle(BusinessActivityFailEvent event) {
		status = BusinessActivityStatus.FAILING;
		LOG.info("RTS-BA SAGA-ACTV :: Coordination context {} :: Activity {} >> FAIL", coordCtxId, activityId);
		
		if (participant.getProtocolEvents().contains(RtsBaMessage.FAILED)){
			send(BusinessActivityMessageContentType.FAILED);
		}
		
		// we must generate a FAILED event in order to end the business activity saga
		bap.failed(coordCtxId, activityId, BusinessActivityMessageType.FAILED);
	}

	@SagaEventHandler(associationProperty = ASSOCIATION_PROPERTY)
	public void handle(BusinessActivityCancelEvent event) {
		status = BusinessActivityStatus.CANCELING;
		LOG.info("RTS-BA SAGA-ACTV :: Coordination context {} :: Activity {} << CANCEL", coordCtxId, activityId);
		
		if (participant.getProtocolEvents().contains(RtsBaMessage.CANCEL)){
			send(BusinessActivityMessageContentType.CANCEL);
		} else {
			// we must generate a CANCELED event in order to end the business activity saga
			bap.canceled(coordCtxId, activityId, BusinessActivityMessageType.CANCELED);
		}
	}
	
	@SagaEventHandler(associationProperty = ASSOCIATION_PROPERTY)
	public void handle(BusinessActivityCloseEvent event) {		
		LOG.info("RTS-BA SAGA-ACTV :: Coordination context {} :: Activity {} << CLOSE", coordCtxId, activityId);
		status = BusinessActivityStatus.CLOSING;
		
		if (participant.getProtocolEvents().contains(RtsBaMessage.CLOSE)){
			send(BusinessActivityMessageContentType.CLOSE);
		} else {
			// we must generate a CLOSED event in order to end the business activity saga
			bap.closed(coordCtxId, activityId, BusinessActivityMessageType.CLOSED);
		}
	}

	@SagaEventHandler(associationProperty = ASSOCIATION_PROPERTY)
	public void handle(BusinessActivityCompensateEvent event) {		
		LOG.info("RTS-BA SAGA-ACTV :: Coordination context {} :: Activity {} << COMPENSATE", coordCtxId, activityId);
		status = BusinessActivityStatus.COMPENSATING;
		
		// Compensation call is done at transaction level
//		if (participant.getProtocolEvents().contains(RtsBaMessage.COMPENSATE)){
//			send(BusinessActivityMessageContentType.COMPENSATE);
//		} else {
//			// we must generate a COMPENSATED event in order to end the business activity saga
//			bap.compensated(txId, activityId, BusinessActivityMessageType.COMPENSATED);
//		}
	}

	@SagaEventHandler(associationProperty = ASSOCIATION_PROPERTY)
	public void handle(BusinessActivityFailCompensatingEvent event) {
		status = BusinessActivityStatus.FAILING;
		LOG.info("RTS-BA SAGA-ACTV :: Coordination context {} :: Activity {} >> FAIL COMPENSATING", coordCtxId, activityId);

		if (participant.getProtocolEvents().contains(RtsBaMessage.FAILED)){
			send(BusinessActivityMessageContentType.FAILED);
		}
		
		// we must generate a FAILED event in order to end the business activity saga
		bap.failed(coordCtxId, activityId, BusinessActivityMessageType.FAILED);
	}

	@SagaEventHandler(associationProperty = ASSOCIATION_PROPERTY)
	public void handle(BusinessActivityFailCancelingEvent event) {
		status = BusinessActivityStatus.FAILING;
		LOG.info("RTS-BA SAGA-ACTV :: Coordination context {} :: Activity {} >> FAIL CANCELING", coordCtxId, activityId);
		
		if (participant.getProtocolEvents().contains(RtsBaMessage.FAILED)){
			send(BusinessActivityMessageContentType.FAILED);
		}
		
		// we must generate a FAILED event in order to end the business activity saga
		bap.failed(coordCtxId, activityId, BusinessActivityMessageType.FAILED);
	}
	
	@SagaEventHandler(associationProperty = ASSOCIATION_PROPERTY)
	@EndSaga
	public void handle(BusinessActivityClosedEvent event) {
		status = BusinessActivityStatus.ENDED;
		LOG.info("RTS-BA SAGA-ACTV :: Coordination context {} :: Activity {} >> CLOSED", coordCtxId, activityId);
	}
	
	@SagaEventHandler(associationProperty = ASSOCIATION_PROPERTY)
	@EndSaga
	public void handle(BusinessActivityCompensatedEvent event) {
		status = BusinessActivityStatus.ENDED;
		LOG.info("RTS-BA SAGA-ACTV :: Coordination context {} :: Activity {} >> COMPENSATED", coordCtxId, activityId);
	}

	@SagaEventHandler(associationProperty = ASSOCIATION_PROPERTY)
	@EndSaga
	public void handle(BusinessActivityFailedEvent event) {		
		LOG.info("RTS-BA SAGA-ACTV :: Coordination context {} :: Activity {} << FAILED", coordCtxId, activityId);
		status = BusinessActivityStatus.ENDED;
	}
	
	@SagaEventHandler(associationProperty = ASSOCIATION_PROPERTY)
	@EndSaga
	public void handle(BusinessActivityExitedEvent event) {		
		LOG.info("RTS-BA SAGA-ACTV :: Coordination context {} :: Activity {} << EXITED", coordCtxId, activityId);
		status = BusinessActivityStatus.ENDED;
	}

	@SagaEventHandler(associationProperty = ASSOCIATION_PROPERTY)
	@EndSaga
	public void handle(BusinessActivityCanceledEvent event) {		
		LOG.info("RTS-BA SAGA-ACTV :: Coordination context {} :: Activity {} >> CANCELED", coordCtxId, activityId);
		status = BusinessActivityStatus.ENDED;
	}
	
	@SagaEventHandler(associationProperty = ASSOCIATION_PROPERTY)
	@EndSaga
	public void handle(BusinessActivityNotCompletedEvent event) {		
		LOG.info("RTS-BA SAGA-ACTV :: Coordination context {} :: Activity {} << NOT COMPLETED", coordCtxId, activityId);
		status = BusinessActivityStatus.ENDED;
	}

	public BusinessActivityStatus getStatus() {
		return status;
	}

	public CoordinationContextParticipant getParticipant() {
		return participant;
	}
}
