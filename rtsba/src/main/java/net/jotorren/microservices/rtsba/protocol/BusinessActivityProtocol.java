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
	
	public void activation(String coordCtxId, String activityId, CoordinationContextParticipant participant, long activitySequence){
		BusinessActivityActivationEvent event = new BusinessActivityActivationEvent(coordCtxId, activityId, participant);
		
		GenericDomainEventMessage<BusinessActivityActivationEvent> msg = 
				new GenericDomainEventMessage<>("ba-activation", 
						event.getActivityInstanceId(), activitySequence, event);
		sagaBus.publish(GenericDomainEventMessage.asEventMessage(msg));
	}
	
	public void cancel(String coordCtxId, String activityId, long activitySequence){
		BusinessActivityCancelEvent event = new BusinessActivityCancelEvent(coordCtxId, activityId);
		
		GenericDomainEventMessage<BusinessActivityCancelEvent> msg = 
				new GenericDomainEventMessage<>("ba-cancel", 
						event.getActivityInstanceId(), activitySequence, event);
		sagaBus.publish(GenericDomainEventMessage.asEventMessage(msg));
	}

	public void canceled(String coordCtxId, String activityId, long activitySequence){
		BusinessActivityCanceledEvent event = new BusinessActivityCanceledEvent(coordCtxId, activityId);
		
		GenericDomainEventMessage<BusinessActivityCanceledEvent> msg = 
				new GenericDomainEventMessage<>("ba-canceled", 
						event.getActivityInstanceId(), activitySequence, event);
		sagaBus.publish(GenericDomainEventMessage.asEventMessage(msg));
	}

	public void cannotComplete(String coordCtxId, String activityId, long activitySequence){
		BusinessActivityCannotCompleteEvent event = new BusinessActivityCannotCompleteEvent(coordCtxId, activityId);
		
		GenericDomainEventMessage<BusinessActivityCannotCompleteEvent> msg = 
				new GenericDomainEventMessage<>("ba-cannotComplete", 
						event.getActivityInstanceId(), activitySequence, event);
		sagaBus.publish(GenericDomainEventMessage.asEventMessage(msg));
	}
	
	public void close(String coordCtxId, String activityId, long activitySequence){
		BusinessActivityCloseEvent event = new BusinessActivityCloseEvent(coordCtxId, activityId);
		
		GenericDomainEventMessage<BusinessActivityCloseEvent> msg = 
				new GenericDomainEventMessage<>("ba-close", 
						event.getActivityInstanceId(), activitySequence, event);
		sagaBus.publish(GenericDomainEventMessage.asEventMessage(msg));
	}

	public void closed(String coordCtxId, String activityId, long activitySequence){
		BusinessActivityClosedEvent event = new BusinessActivityClosedEvent(coordCtxId, activityId);
		
		GenericDomainEventMessage<BusinessActivityClosedEvent> msg = 
				new GenericDomainEventMessage<>("ba-closed", 
						event.getActivityInstanceId(), activitySequence, event);
		sagaBus.publish(GenericDomainEventMessage.asEventMessage(msg));
	}
	
	public void compensate(String coordCtxId, String activityId, long activitySequence){
		BusinessActivityCompensateEvent event = new BusinessActivityCompensateEvent(coordCtxId, activityId);
		
		GenericDomainEventMessage<BusinessActivityCompensateEvent> msg = 
				new GenericDomainEventMessage<>("ba-compensate", 
						event.getActivityInstanceId(), activitySequence, event);
		sagaBus.publish(GenericDomainEventMessage.asEventMessage(msg));
	}

	public void compensated(String coordCtxId, String activityId, long activitySequence){
		BusinessActivityCompensatedEvent event = new BusinessActivityCompensatedEvent(coordCtxId, activityId);
		
		GenericDomainEventMessage<BusinessActivityCompensatedEvent> msg = 
				new GenericDomainEventMessage<>("ba-compensated", 
						event.getActivityInstanceId(), activitySequence, event);
		sagaBus.publish(GenericDomainEventMessage.asEventMessage(msg));
	}
	
	public void completed(String coordCtxId, String activityId, long activitySequence){
		BusinessActivityCompletedEvent event = new BusinessActivityCompletedEvent(coordCtxId, activityId);
		
		GenericDomainEventMessage<BusinessActivityCompletedEvent> msg = 
				new GenericDomainEventMessage<>("ba-completed", 
						event.getActivityInstanceId(), activitySequence, event);
		sagaBus.publish(GenericDomainEventMessage.asEventMessage(msg));
	}
	
	public void exit(String coordCtxId, String activityId, long activitySequence){
		BusinessActivityExitEvent event = new BusinessActivityExitEvent(coordCtxId, activityId);
		
		GenericDomainEventMessage<BusinessActivityExitEvent> msg = 
				new GenericDomainEventMessage<>("ba-exit", event.getActivityInstanceId(), activitySequence, event);
		sagaBus.publish(GenericDomainEventMessage.asEventMessage(msg));
	}

	public void exited(String coordCtxId, String activityId, long activitySequence){
		BusinessActivityExitedEvent event = new BusinessActivityExitedEvent(coordCtxId, activityId);
		
		GenericDomainEventMessage<BusinessActivityExitedEvent> msg = 
				new GenericDomainEventMessage<>("ba-exited", 
						event.getActivityInstanceId(), activitySequence, event);
		sagaBus.publish(GenericDomainEventMessage.asEventMessage(msg));
	}

	public void fail(String coordCtxId, String activityId, long activitySequence){
		BusinessActivityFailEvent event = new BusinessActivityFailEvent(coordCtxId, activityId);
		
		GenericDomainEventMessage<BusinessActivityFailEvent> msg = 
				new GenericDomainEventMessage<>("ba-fail", 
						event.getActivityInstanceId(), activitySequence, event);
		sagaBus.publish(GenericDomainEventMessage.asEventMessage(msg));
	}

	public void failed(String coordCtxId, String activityId, long activitySequence){
		BusinessActivityFailedEvent event = new BusinessActivityFailedEvent(coordCtxId, activityId);
		
		GenericDomainEventMessage<BusinessActivityFailedEvent> msg = 
				new GenericDomainEventMessage<>("ba-failed", 
						event.getActivityInstanceId(), activitySequence, event);
		sagaBus.publish(GenericDomainEventMessage.asEventMessage(msg));
	}

	public void notCompleted(String coordCtxId, String activityId, long activitySequence){
		BusinessActivityNotCompletedEvent event = new BusinessActivityNotCompletedEvent(coordCtxId, activityId);
		
		GenericDomainEventMessage<BusinessActivityNotCompletedEvent> msg = 
				new GenericDomainEventMessage<>("ba-notCompleted", 
						event.getActivityInstanceId(), activitySequence, event);
		sagaBus.publish(GenericDomainEventMessage.asEventMessage(msg));
	}
}
