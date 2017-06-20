package net.jotorren.microservices.rtsba.coordinator.saga;

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
import org.springframework.data.redis.core.StringRedisTemplate;
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

	public transient static final String ASSOCIATION_PROPERTY = "coordinationContextId";
	
	private transient static final Logger LOG = LoggerFactory.getLogger(CoordinationContextSaga.class);

	@Autowired
	private transient EventScheduler scheduler;

	@Autowired
	private transient SchedulerFactoryBean schedulerFactoryBean;

	@Autowired
	private transient CoordinatorSagaService sagaService;

	@Autowired
	private transient StringRedisTemplate redis;
	
	@Autowired
	@Qualifier("rtsbaTemplate")
	private transient RestTemplate restTemplate;
	
	@Autowired
	private transient BusinessActivityProtocol activityProtocol;
	
	private String coordCtxId;
	private String lastStep;
	private boolean expired = false;
	private long timeout = -1;
	private ScheduleToken timeoutToken = null;
	private boolean suspended = false;
	
	private List<CoordinationContextRegistrationEvent> stack = new ArrayList<>();
	
	private void scheduleTimeout() {
		if (timeout >= 0) {
//			timeoutToken = scheduler.schedule(Duration.ofMillis(timeout), 
//			new CoordinationContextTimeoutEvent(txId, timeout));
			
	    	// TODO type and sequence are not stored in the events table
			GenericDomainEventMessage<CoordinationContextTimeoutEvent> msg = 
					new GenericDomainEventMessage<>("rts-timeout", coordCtxId, -1, 
							new CoordinationContextTimeoutEvent(coordCtxId, timeout));
	    	timeoutToken = scheduler.schedule(Duration.ofMillis(timeout), msg);
		}
	}

	private void compensate(String participantUrl, String activityId){
		HttpHeaders headers = new HttpHeaders();
		headers.set(HttpHeaders.CONTENT_TYPE, BusinessActivityMessageContentType.COMPENSATE);

	    Map<String, String> param = new HashMap<String, String>();
	    
		BusinessActivityMessage data = new BusinessActivityMessage(coordCtxId, activityId, BusinessActivityStatus.COMPENSATING);
		
		HttpEntity<BusinessActivityMessage> request = new HttpEntity<BusinessActivityMessage>(data, headers);
		restTemplate.exchange(participantUrl, HttpMethod.PUT, request, Void.class, param);	
	}
	
	@SagaEventHandler(associationProperty = ASSOCIATION_PROPERTY)
	@StartSaga
	public void handle(CoordinationContextOpenEvent event) {
		coordCtxId = event.getCoordinationContextId();
		LOG.info("RTS-BA SAGA-CMTX :: Coordination context {} OPEN", coordCtxId);
        
        timeout = event.getTimeout();
        scheduleTimeout();
        redis.convertAndSend(coordCtxId, "OPEN");
	}

	@SagaEventHandler(associationProperty = ASSOCIATION_PROPERTY)
	public void handle(CoordinationContextRegistrationEvent event) {
		stack.add(event);
		LOG.info("RTS-BA SAGA-CMTX :: Coordination context {} :: Activity {} REGISTERED", coordCtxId, event.getActivityId());
		
		activityProtocol.activation(coordCtxId, event.getActivityId(), event.getParticipant(), BusinessActivityMessageType.REGISTRATION);
	}
	
	@SagaEventHandler(associationProperty = ASSOCIATION_PROPERTY)
	public void handle(CoordinationContextPartialEvent event) {
		lastStep = event.getStepIdentifier();
		LOG.info("RTS-BA SAGA-CMTX :: Coordination context {} :: Activity {} DONE", coordCtxId, event.getStepIdentifier());
	}

	@SagaEventHandler(associationProperty = ASSOCIATION_PROPERTY)
	public void handle(CoordinationContextTimeoutEvent event) {
		expired = true;
		LOG.info("RTS-BA SAGA-CMTX :: Coordination context {} TIMEOUT EXPIRED {} millis", coordCtxId, event.getTimeout());
	}

	@SagaEventHandler(associationProperty = ASSOCIATION_PROPERTY)
	public void handle(CoordinationContextSuspendEvent event) {
		if (!suspended) {
			long now = System.currentTimeMillis();
			LOG.info("RTS-BA SAGA-CMTX :: Coordination context {} SUSPEND", coordCtxId);
			
			if (null != timeoutToken) {
				QuartzScheduleToken quartzToken = (QuartzScheduleToken)timeoutToken;
				JobKey quartzJob = new JobKey(quartzToken.getJobIdentifier(), quartzToken.getGroupIdentifier());
				try {
					List<? extends Trigger> triggers = schedulerFactoryBean.getScheduler().getTriggersOfJob(quartzJob);
					if (triggers.size() == 1) {
						timeout = triggers.get(0).getNextFireTime().getTime() - now;
						scheduler.cancelSchedule(timeoutToken);
						suspended = true;
						LOG.info("RTS-BA SAGA-CMTX :: Coordination context {} SUSPENDED :: Remaining time {}", coordCtxId, timeout);
					} else {
						LOG.error("RTS-BA SAGA-CMTX :: Coordination context {} Unable to suspend timeout");
					}
				} catch (SchedulerException e) {
					LOG.error("RTS-BA SAGA-CMTX :: Coordination context {} Unable to suspend timeout", e);
				}
			}
		}
	}

	@SagaEventHandler(associationProperty = ASSOCIATION_PROPERTY)
	public void handle(CoordinationContextResumeEvent event) {
		if (suspended) {
			scheduleTimeout();
			suspended = false;
			LOG.info("RTS-BA SAGA-CMTX :: Coordination context {} RESUMED :: Remaining time {}", coordCtxId, timeout);
		}
	}
	
	@SagaEventHandler(associationProperty = ASSOCIATION_PROPERTY)
	@EndSaga
	public void handle(CoordinationContextCloseEvent event) {
		LOG.info("RTS-BA SAGA-CMTX :: Coordination context {} CLOSE", coordCtxId);
		if (null != timeoutToken) {
			scheduler.cancelSchedule(timeoutToken);
		}

		for (CoordinationContextRegistrationEvent registered:stack){
			activityProtocol.close(coordCtxId, registered.getActivityId(), BusinessActivityMessageType.CLOSE);
		}
	}
	
	@SagaEventHandler(associationProperty = ASSOCIATION_PROPERTY)
	@EndSaga
	public void handle(CoordinationContextCompensateEvent event) {
		LOG.info("RTS-BA SAGA-CMTX :: Coordination context {} COMPENSATE", coordCtxId);
		if (null != timeoutToken && !expired) {
			scheduler.cancelSchedule(timeoutToken);
		}

		ListIterator<CoordinationContextRegistrationEvent> iterator = stack.listIterator(stack.size());
		while (iterator.hasPrevious()) {
			final CoordinationContextRegistrationEvent register = iterator.previous();
			final BusinessActivityStatus status = sagaService.getBusinessActivityStatus(coordCtxId + "-" + register.getActivityId());
			
			if (BusinessActivityStatus.COMPLETED.equals(status)){
				activityProtocol.compensate(coordCtxId, register.getActivityId(), BusinessActivityMessageType.COMPENSATE);		  
				if (register.getParticipant().getProtocolEvents().contains(RtsBaMessage.COMPENSATE)){
					try {
						compensate(register.getParticipant().getAddress(), register.getActivityId());
					} catch (Exception e){
						LOG.error("Heuristic compensate error for activity " + register.getActivityId(), e);
					}
				} else {
					// we must generate a COMPENSATED event in order to end the business activity saga
					activityProtocol.compensated(coordCtxId, register.getActivityId(), BusinessActivityMessageType.COMPENSATED);
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
