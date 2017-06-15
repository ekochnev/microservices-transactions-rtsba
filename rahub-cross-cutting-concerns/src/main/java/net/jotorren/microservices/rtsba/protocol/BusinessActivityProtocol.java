package net.jotorren.microservices.rtsba.protocol;

import org.axonframework.eventhandling.EventBus;
import org.axonframework.eventsourcing.GenericDomainEventMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import net.jotorren.microservices.rtsba.CoordinationContextParticipant;
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
import net.jotorren.microservices.rtsba.protocol.event.BusinessActivityFailEvent;
import net.jotorren.microservices.rtsba.protocol.event.BusinessActivityFailedEvent;
import net.jotorren.microservices.rtsba.protocol.event.BusinessActivityNotCompletedEvent;

/**
 * Exchangeable messages between a SINGLE business activity and a RTS-BA Coordinator 
 *
 */
@Component
public class BusinessActivityProtocol {

	@Autowired
	private EventBus sagaBus;
	
	public void activation(String txId, String activityId, CoordinationContextParticipant participant, long actSeqNumber){
		BusinessActivityActivationEvent event = new BusinessActivityActivationEvent(txId, activityId, participant, actSeqNumber);
		
		GenericDomainEventMessage<BusinessActivityActivationEvent> msg = 
				new GenericDomainEventMessage<>("ba-activation", event.getActivityInstanceId(), actSeqNumber, event);
		sagaBus.publish(GenericDomainEventMessage.asEventMessage(msg));
	}
	
	public void cancel(String txId, String activityId, long actSeqNumber){
		BusinessActivityCancelEvent event = new BusinessActivityCancelEvent(txId, activityId);
		
		GenericDomainEventMessage<BusinessActivityCancelEvent> msg = 
				new GenericDomainEventMessage<>("ba-cancel", event.getActivityInstanceId(), actSeqNumber, event);
		sagaBus.publish(GenericDomainEventMessage.asEventMessage(msg));
	}

	public void canceled(String txId, String activityId, long actSeqNumber){
		BusinessActivityCanceledEvent event = new BusinessActivityCanceledEvent(txId, activityId);
		
		GenericDomainEventMessage<BusinessActivityCanceledEvent> msg = 
				new GenericDomainEventMessage<>("ba-canceled", event.getActivityInstanceId(), actSeqNumber, event);
		sagaBus.publish(GenericDomainEventMessage.asEventMessage(msg));
	}

	public void cannotComplete(String txId, String activityId, long actSeqNumber){
		BusinessActivityCannotCompleteEvent event = new BusinessActivityCannotCompleteEvent(txId, activityId);
		
		GenericDomainEventMessage<BusinessActivityCannotCompleteEvent> msg = 
				new GenericDomainEventMessage<>("ba-cannotComplete", event.getActivityInstanceId(), actSeqNumber, event);
		sagaBus.publish(GenericDomainEventMessage.asEventMessage(msg));
	}
	
	public void close(String txId, String activityId, long actSeqNumber){
		BusinessActivityCloseEvent event = new BusinessActivityCloseEvent(txId, activityId);
		
		GenericDomainEventMessage<BusinessActivityCloseEvent> msg = 
				new GenericDomainEventMessage<>("ba-close", event.getActivityInstanceId(), actSeqNumber, event);
		sagaBus.publish(GenericDomainEventMessage.asEventMessage(msg));
	}

	public void closed(String txId, String activityId, long actSeqNumber){
		BusinessActivityClosedEvent event = new BusinessActivityClosedEvent(txId, activityId);
		
		GenericDomainEventMessage<BusinessActivityClosedEvent> msg = 
				new GenericDomainEventMessage<>("ba-closed", event.getActivityInstanceId(), actSeqNumber, event);
		sagaBus.publish(GenericDomainEventMessage.asEventMessage(msg));
	}
	
	public void compensate(String txId, String activityId, long actSeqNumber){
		BusinessActivityCompensateEvent event = new BusinessActivityCompensateEvent(txId, activityId);
		
		GenericDomainEventMessage<BusinessActivityCompensateEvent> msg = 
				new GenericDomainEventMessage<>("ba-compensate", event.getActivityInstanceId(), actSeqNumber, event);
		sagaBus.publish(GenericDomainEventMessage.asEventMessage(msg));
	}

	public void compensated(String txId, String activityId, long actSeqNumber){
		BusinessActivityCompensatedEvent event = new BusinessActivityCompensatedEvent(txId, activityId);
		
		GenericDomainEventMessage<BusinessActivityCompensatedEvent> msg = 
				new GenericDomainEventMessage<>("ba-compensated", event.getActivityInstanceId(), actSeqNumber, event);
		sagaBus.publish(GenericDomainEventMessage.asEventMessage(msg));
	}
	
	public void completed(String txId, String activityId, long actSeqNumber){
		BusinessActivityCompletedEvent event = new BusinessActivityCompletedEvent(txId, activityId);
		
		GenericDomainEventMessage<BusinessActivityCompletedEvent> msg = 
				new GenericDomainEventMessage<>("ba-completed", event.getActivityInstanceId(), actSeqNumber, event);
		sagaBus.publish(GenericDomainEventMessage.asEventMessage(msg));
	}
	
	public void exit(String txId, String activityId, long actSeqNumber){
		BusinessActivityExitEvent event = new BusinessActivityExitEvent(txId, activityId);
		
		GenericDomainEventMessage<BusinessActivityExitEvent> msg = 
				new GenericDomainEventMessage<>("ba-exit", event.getActivityInstanceId(), actSeqNumber, event);
		sagaBus.publish(GenericDomainEventMessage.asEventMessage(msg));
	}

	public void exited(String txId, String activityId, long actSeqNumber){
		BusinessActivityExitedEvent event = new BusinessActivityExitedEvent(txId, activityId);
		
		GenericDomainEventMessage<BusinessActivityExitedEvent> msg = 
				new GenericDomainEventMessage<>("ba-exited", event.getActivityInstanceId(), actSeqNumber, event);
		sagaBus.publish(GenericDomainEventMessage.asEventMessage(msg));
	}

	public void fail(String txId, String activityId, long actSeqNumber){
		BusinessActivityFailEvent event = new BusinessActivityFailEvent(txId, activityId);
		
		GenericDomainEventMessage<BusinessActivityFailEvent> msg = 
				new GenericDomainEventMessage<>("ba-fail", event.getActivityInstanceId(), actSeqNumber, event);
		sagaBus.publish(GenericDomainEventMessage.asEventMessage(msg));
	}

	public void failed(String txId, String activityId, long actSeqNumber){
		BusinessActivityFailedEvent event = new BusinessActivityFailedEvent(txId, activityId);
		
		GenericDomainEventMessage<BusinessActivityFailedEvent> msg = 
				new GenericDomainEventMessage<>("ba-failed", event.getActivityInstanceId(), actSeqNumber, event);
		sagaBus.publish(GenericDomainEventMessage.asEventMessage(msg));
	}

	public void notCompleted(String txId, String activityId, long actSeqNumber){
		BusinessActivityNotCompletedEvent event = new BusinessActivityNotCompletedEvent(txId, activityId);
		
		GenericDomainEventMessage<BusinessActivityNotCompletedEvent> msg = 
				new GenericDomainEventMessage<>("ba-notCompleted", event.getActivityInstanceId(), actSeqNumber, event);
		sagaBus.publish(GenericDomainEventMessage.asEventMessage(msg));
	}
}
