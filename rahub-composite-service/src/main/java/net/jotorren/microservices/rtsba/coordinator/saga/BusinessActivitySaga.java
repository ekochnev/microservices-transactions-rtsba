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

	private transient static final Logger LOG = LoggerFactory.getLogger(BusinessActivitySaga.class);

	@Autowired
	private transient StringRedisTemplate redis;

	@Autowired
	@Qualifier("rtsbaTemplate")
	private transient RestTemplate restTemplate;

	@Autowired
	private transient BusinessActivityProtocol bap;
	
	private String txId;
	private String activityId;
	private BusinessActivityStatus status;
	private CoordinationContextParticipant participant;
	
	private void send(String contentType) {
		HttpHeaders headers = new HttpHeaders();
		headers.set(HttpHeaders.CONTENT_TYPE, contentType);

	    Map<String, String> param = new HashMap<String, String>();
	    
		BusinessActivityMessage data = new BusinessActivityMessage(this.txId, this.activityId, this.status);
		
		HttpEntity<BusinessActivityMessage> request = new HttpEntity<BusinessActivityMessage>(data, headers);
		this.restTemplate.exchange(this.participant.getAddress(), HttpMethod.PUT, request, Void.class, param);	
	}
	
	@SagaEventHandler(associationProperty = "activityInstanceId")
	@StartSaga
	public void handle(BusinessActivityActivationEvent event) {
		this.txId = event.getCompositeTransactionId();
		this.activityId = event.getActivityId();
		this.participant = event.getParticipant();
		this.status = BusinessActivityStatus.ACTIVE;
		
		this.redis.convertAndSend(this.txId + "-" + this.activityId, BusinessActivityStatus.ACTIVE.toString());
		LOG.info("RTS-BA SAGA-ACTV :: Transaction {} :: Activity {} << ACTIVE", this.txId, this.activityId);
	}

	@SagaEventHandler(associationProperty = "activityInstanceId")
	public void handle(BusinessActivityExitEvent event) {
		this.status = BusinessActivityStatus.EXITING;
		LOG.info("RTS-BA SAGA-ACTV :: Transaction {} :: Activity {} >> EXIT", this.txId, this.activityId);

		if (this.participant.getProtocolEvents().contains(RtsBaMessage.EXITED)){
			send(BusinessActivityMessageContentType.EXITED);
		}
		
		// we must generate a EXITED event in order to end the business activity saga
		this.bap.exited(this.txId, this.activityId, BusinessActivityMessageType.EXITED);	
	}

	@SagaEventHandler(associationProperty = "activityInstanceId")
	public void handle(BusinessActivityCannotCompleteEvent event) {
		this.status = BusinessActivityStatus.NOTCOMPLETING;
		LOG.info("RTS-BA SAGA-ACTV :: Transaction {} :: Activity {} >> CANNOT COMPLETE", this.txId, this.activityId);
		
		if (this.participant.getProtocolEvents().contains(RtsBaMessage.NOT_COMPLETED)){
			send(BusinessActivityMessageContentType.NOT_COMPLETED);
		}
		
		// we must generate a NOT COMPLETED event in order to end the business activity saga
		this.bap.notCompleted(this.txId, this.activityId, BusinessActivityMessageType.NOT_COMPLETED);	
	}
	
	@SagaEventHandler(associationProperty = "activityInstanceId")
	public void handle(BusinessActivityCompletedEvent event) {
		this.status = BusinessActivityStatus.COMPLETED;
		LOG.info("RTS-BA SAGA-ACTV :: Transaction {} :: Activity {} >> COMPLETED", this.txId, this.activityId);
	}

	@SagaEventHandler(associationProperty = "activityInstanceId")
	public void handle(BusinessActivityFailEvent event) {
		this.status = BusinessActivityStatus.FAILING;
		LOG.info("RTS-BA SAGA-ACTV :: Transaction {} :: Activity {} >> FAIL", this.txId, this.activityId);
		
		if (this.participant.getProtocolEvents().contains(RtsBaMessage.FAILED)){
			send(BusinessActivityMessageContentType.FAILED);
		}
		
		// we must generate a FAILED event in order to end the business activity saga
		this.bap.failed(this.txId, this.activityId, BusinessActivityMessageType.FAILED);
	}

	@SagaEventHandler(associationProperty = "activityInstanceId")
	public void handle(BusinessActivityCancelEvent event) {
		this.status = BusinessActivityStatus.CANCELING;
		LOG.info("RTS-BA SAGA-ACTV :: Transaction {} :: Activity {} << CANCEL", this.txId, this.activityId);
		
		if (this.participant.getProtocolEvents().contains(RtsBaMessage.CANCEL)){
			send(BusinessActivityMessageContentType.CANCEL);
		} else {
			// we must generate a CANCELED event in order to end the business activity saga
			this.bap.canceled(this.txId, this.activityId, BusinessActivityMessageType.CANCELED);
		}
	}
	
	@SagaEventHandler(associationProperty = "activityInstanceId")
	public void handle(BusinessActivityCloseEvent event) {		
		LOG.info("RTS-BA SAGA-ACTV :: Transaction {} :: Activity {} << CLOSE", this.txId, this.activityId);
		this.status = BusinessActivityStatus.CLOSING;
		
		if (this.participant.getProtocolEvents().contains(RtsBaMessage.CLOSE)){
			send(BusinessActivityMessageContentType.CLOSE);
		} else {
			// we must generate a CLOSED event in order to end the business activity saga
			this.bap.closed(this.txId, this.activityId, BusinessActivityMessageType.CLOSED);
		}
	}

	@SagaEventHandler(associationProperty = "activityInstanceId")
	public void handle(BusinessActivityCompensateEvent event) {		
		LOG.info("RTS-BA SAGA-ACTV :: Transaction {} :: Activity {} << COMPENSATE", this.txId, this.activityId);
		this.status = BusinessActivityStatus.COMPENSATING;
		
		// Compensation call is done at transaction level
//		if (this.participant.getProtocolEvents().contains(RtsBaMessage.COMPENSATE)){
//			send(BusinessActivityMessageContentType.COMPENSATE);
//		} else {
//			// we must generate a COMPENSATED event in order to end the business activity saga
//			this.bap.compensated(this.txId, this.activityId, BusinessActivityMessageType.COMPENSATED);
//		}
	}

	@SagaEventHandler(associationProperty = "activityInstanceId")
	public void handle(BusinessActivityFailCompensatingEvent event) {
		this.status = BusinessActivityStatus.FAILING;
		LOG.info("RTS-BA SAGA-ACTV :: Transaction {} :: Activity {} >> FAIL COMPENSATING", this.txId, this.activityId);

		if (this.participant.getProtocolEvents().contains(RtsBaMessage.FAILED)){
			send(BusinessActivityMessageContentType.FAILED);
		}
		
		// we must generate a FAILED event in order to end the business activity saga
		this.bap.failed(this.txId, this.activityId, BusinessActivityMessageType.FAILED);
	}

	@SagaEventHandler(associationProperty = "activityInstanceId")
	public void handle(BusinessActivityFailCancelingEvent event) {
		this.status = BusinessActivityStatus.FAILING;
		LOG.info("RTS-BA SAGA-ACTV :: Transaction {} :: Activity {} >> FAIL CANCELING", this.txId, this.activityId);
		
		if (this.participant.getProtocolEvents().contains(RtsBaMessage.FAILED)){
			send(BusinessActivityMessageContentType.FAILED);
		}
		
		// we must generate a FAILED event in order to end the business activity saga
		this.bap.failed(this.txId, this.activityId, BusinessActivityMessageType.FAILED);
	}
	
	@SagaEventHandler(associationProperty = "activityInstanceId")
	@EndSaga
	public void handle(BusinessActivityClosedEvent event) {
		this.status = BusinessActivityStatus.ENDED;
		LOG.info("RTS-BA SAGA-ACTV :: Transaction {} :: Activity {} >> CLOSED", this.txId, this.activityId);
	}
	
	@SagaEventHandler(associationProperty = "activityInstanceId")
	@EndSaga
	public void handle(BusinessActivityCompensatedEvent event) {
		this.status = BusinessActivityStatus.ENDED;
		LOG.info("RTS-BA SAGA-ACTV :: Transaction {} :: Activity {} >> COMPENSATED", this.txId, this.activityId);
	}

	@SagaEventHandler(associationProperty = "activityInstanceId")
	@EndSaga
	public void handle(BusinessActivityFailedEvent event) {		
		LOG.info("RTS-BA SAGA-ACTV :: Transaction {} :: Activity {} << FAILED", this.txId, this.activityId);
		this.status = BusinessActivityStatus.ENDED;
	}
	
	@SagaEventHandler(associationProperty = "activityInstanceId")
	@EndSaga
	public void handle(BusinessActivityExitedEvent event) {		
		LOG.info("RTS-BA SAGA-ACTV :: Transaction {} :: Activity {} << EXITED", this.txId, this.activityId);
		this.status = BusinessActivityStatus.ENDED;
	}

	@SagaEventHandler(associationProperty = "activityInstanceId")
	@EndSaga
	public void handle(BusinessActivityCanceledEvent event) {		
		LOG.info("RTS-BA SAGA-ACTV :: Transaction {} :: Activity {} >> CANCELED", this.txId, this.activityId);
		this.status = BusinessActivityStatus.ENDED;
	}
	
	@SagaEventHandler(associationProperty = "activityInstanceId")
	@EndSaga
	public void handle(BusinessActivityNotCompletedEvent event) {		
		LOG.info("RTS-BA SAGA-ACTV :: Transaction {} :: Activity {} << NOT COMPLETED", this.txId, this.activityId);
		this.status = BusinessActivityStatus.ENDED;
	}

	public BusinessActivityStatus getStatus() {
		return status;
	}

	public CoordinationContextParticipant getParticipant() {
		return participant;
	}
}
