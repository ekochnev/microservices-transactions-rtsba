package net.jotorren.microservices.rtsba.coordinator;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.axonframework.eventhandling.saga.EndSaga;
import org.axonframework.eventhandling.saga.SagaEventHandler;
import org.axonframework.eventhandling.saga.StartSaga;
import org.axonframework.eventhandling.scheduling.EventScheduler;
import org.axonframework.eventhandling.scheduling.ScheduleToken;
import org.axonframework.eventhandling.scheduling.quartz.QuartzScheduleToken;
import org.axonframework.eventsourcing.GenericDomainEventMessage;
import org.axonframework.spring.stereotype.Saga;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.web.client.RestTemplate;

import net.jotorren.microservices.rtsba.protocol.BusinessActivityMessage;
import net.jotorren.microservices.rtsba.protocol.BusinessActivityMessageContentType;
import net.jotorren.microservices.rtsba.protocol.BusinessActivityMessageType;
import net.jotorren.microservices.rtsba.protocol.BusinessActivityProtocol;
import net.jotorren.microservices.rtsba.protocol.BusinessActivityStatus;
import net.jotorren.microservices.rtsba.protocol.RtsBaMessage;
import net.jotorren.microservices.rtsba.protocol.event.CoordinationContextCloseEvent;
import net.jotorren.microservices.rtsba.protocol.event.CoordinationContextCompensateEvent;
import net.jotorren.microservices.rtsba.protocol.event.CoordinationContextOpenEvent;
import net.jotorren.microservices.rtsba.protocol.event.CoordinationContextPartialEvent;
import net.jotorren.microservices.rtsba.protocol.event.CoordinationContextRegistrationEvent;
import net.jotorren.microservices.rtsba.protocol.event.CoordinationContextResumeEvent;
import net.jotorren.microservices.rtsba.protocol.event.CoordinationContextSuspendEvent;
import net.jotorren.microservices.rtsba.protocol.event.CoordinationContextTimeoutEvent;

@Saga
public class CoordinationContextSaga {

	private transient static final Logger LOG = LoggerFactory.getLogger(CoordinationContextSaga.class);

	@Autowired
	private transient EventScheduler scheduler;

	@Autowired
	private transient SchedulerFactoryBean schedulerFactoryBean;

	@Autowired
	private transient CoordinatorSagaService sagaService;
	
	@Autowired
	@Qualifier("rtsbaTemplate")
	private transient RestTemplate restTemplate;
	
	@Autowired
	private transient BusinessActivityProtocol activityProtocol;
	
	private String txId;
	private String lastStep;
	private boolean expired = false;
	private long timeout = -1;
	private ScheduleToken timeoutToken = null;
	private boolean suspended = false;
	
	private List<CoordinationContextRegistrationEvent> stack = new ArrayList<>();
	
	private void scheduleTimeout() {
		if (this.timeout >= 0) {
//			this.timeoutToken = scheduler.schedule(Duration.ofMillis(this.timeout), 
//			new CoordinationContextTimeoutEvent(this.txId, this.timeout));
			
	    	// TODO type and sequence are not stored in the events table
			GenericDomainEventMessage<CoordinationContextTimeoutEvent> msg = 
					new GenericDomainEventMessage<>("rts-timeout", this.txId, -1, 
							new CoordinationContextTimeoutEvent(this.txId, this.timeout));
	    	this.timeoutToken = this.scheduler.schedule(Duration.ofMillis(this.timeout), msg);
		}
	}

	private void compensate(String participantUrl, String activityId){
		HttpHeaders headers = new HttpHeaders();
		headers.set(HttpHeaders.CONTENT_TYPE, BusinessActivityMessageContentType.COMPENSATE);

	    Map<String, String> param = new HashMap<String, String>();
	    
		BusinessActivityMessage data = new BusinessActivityMessage(this.txId, activityId, BusinessActivityStatus.COMPENSATING);
		
		HttpEntity<BusinessActivityMessage> request = new HttpEntity<BusinessActivityMessage>(data, headers);
		this.restTemplate.exchange(participantUrl, HttpMethod.PUT, request, Void.class, param);	
	}
	
	@SagaEventHandler(associationProperty = "compositeTransactionId")
	@StartSaga
	public void handle(CoordinationContextOpenEvent event) {
		this.txId = event.getCompositeTransactionId();
		LOG.info("RTS-BA SAGA-CMTX :: Transaction {} OPEN", this.txId);
        
        this.timeout = event.getTimeout();
        this.scheduleTimeout();
	}

	@SagaEventHandler(associationProperty = "compositeTransactionId")
	public void handle(CoordinationContextRegistrationEvent event) {
		stack.add(event);
		LOG.info("RTS-BA SAGA-CMTX :: Transaction {} :: Activity {} REGISTERED", this.txId, event.getActivityId());
		
		activityProtocol.activation(txId, event.getActivityId(), event.getParticipant(), BusinessActivityMessageType.REGISTRATION);
	}
	
	@SagaEventHandler(associationProperty = "compositeTransactionId")
	public void handle(CoordinationContextPartialEvent event) {
		this.lastStep = event.getStepIdentifier();
		LOG.info("RTS-BA SAGA-CMTX :: Transaction {} STEP {} DONE", this.txId, event.getStepIdentifier());
	}

	@SagaEventHandler(associationProperty = "compositeTransactionId")
	public void handle(CoordinationContextTimeoutEvent event) {
		this.expired = true;
		LOG.info("RTS-BA SAGA-CMTX :: Transaction {} TIMEOUT EXPIRED {} millis", this.txId, event.getTimeout());
	}

	@SagaEventHandler(associationProperty = "compositeTransactionId")
	public void handle(CoordinationContextSuspendEvent event) {
		if (!this.suspended) {
			long now = System.currentTimeMillis();
			LOG.info("RTS-BA SAGA-CMTX :: Transaction {} SUSPEND", this.txId);
			
			if (null != this.timeoutToken) {
				QuartzScheduleToken quartzToken = (QuartzScheduleToken)this.timeoutToken;
				JobKey quartzJob = new JobKey(quartzToken.getJobIdentifier(), quartzToken.getGroupIdentifier());
				try {
					List<? extends Trigger> triggers = this.schedulerFactoryBean.getScheduler().getTriggersOfJob(quartzJob);
					if (triggers.size() == 1) {
						this.timeout = triggers.get(0).getNextFireTime().getTime() - now;
						this.scheduler.cancelSchedule(this.timeoutToken);
						this.suspended = true;
						LOG.info("RTS-BA SAGA-CMTX :: Transaction {} SUSPENDED :: Remaining time {}", this.txId, this.timeout);
					} else {
						LOG.error("RTS-BA SAGA-CMTX :: Transaction {} Unable to suspend timeout");
					}
				} catch (SchedulerException e) {
					LOG.error("RTS-BA SAGA-CMTX :: Transaction {} Unable to suspend timeout", e);
				}
			}
		}
	}

	@SagaEventHandler(associationProperty = "compositeTransactionId")
	public void handle(CoordinationContextResumeEvent event) {
		if (this.suspended) {
			this.scheduleTimeout();
			this.suspended = false;
			LOG.info("RTS-BA SAGA-CMTX :: Transaction {} RESUMED :: Remaining time {}", this.txId, this.timeout);
		}
	}
	
	@SagaEventHandler(associationProperty = "compositeTransactionId")
	@EndSaga
	public void handle(CoordinationContextCloseEvent event) {
		LOG.info("RTS-BA SAGA-CMTX :: Transaction {} CLOSE", this.txId);
		if (null != timeoutToken) {
			scheduler.cancelSchedule(timeoutToken);
		}

		for (CoordinationContextRegistrationEvent registered:stack){
			activityProtocol.close(txId, registered.getActivityId(), BusinessActivityMessageType.CLOSE);
		}
	}
	
	@SagaEventHandler(associationProperty = "compositeTransactionId")
	@EndSaga
	public void handle(CoordinationContextCompensateEvent event) {
		LOG.info("RTS-BA SAGA-CMTX :: Transaction {} COMPENSATE", this.txId);
		if (null != timeoutToken && !expired) {
			scheduler.cancelSchedule(timeoutToken);
		}

		ListIterator<CoordinationContextRegistrationEvent> iterator = stack.listIterator(stack.size());
		while (iterator.hasPrevious()) {
			final CoordinationContextRegistrationEvent register = iterator.previous();
			final BusinessActivityStatus status = sagaService.getBusinessActivityStatus(this.txId + "-" + register.getActivityId());
			
			if (BusinessActivityStatus.COMPLETED.equals(status)){
				activityProtocol.compensate(this.txId, register.getActivityId(), BusinessActivityMessageType.COMPENSATE);		  
				if (register.getParticipant().getProtocolEvents().contains(RtsBaMessage.COMPENSATE)){
					compensate(register.getParticipant().getAddress(), register.getActivityId());
				} else {
					// we must generate a COMPENSATED event in order to end the business activity saga
					this.activityProtocol.compensated(this.txId, register.getActivityId(), BusinessActivityMessageType.COMPENSATED);
				}
			}
		}	
	}

	public String getLastStep() {
		return lastStep;
	}

	public ScheduleToken getTimeout() {
		return timeoutToken;
	}

	public boolean isExpired() {
		return expired;
	}
}
