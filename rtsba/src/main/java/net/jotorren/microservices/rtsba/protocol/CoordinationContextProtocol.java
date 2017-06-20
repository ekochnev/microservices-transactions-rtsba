package net.jotorren.microservices.rtsba.protocol;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.axonframework.eventhandling.EventBus;
import org.axonframework.eventsourcing.GenericDomainEventMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionTimedOutException;

import net.jotorren.microservices.rtsba.CoordinationContextParticipant;
import net.jotorren.microservices.rtsba.RtsBaProperties;
import net.jotorren.microservices.rtsba.participant.error.RtsBaException;
import net.jotorren.microservices.rtsba.protocol.event.CoordinationContextCancelEvent;
import net.jotorren.microservices.rtsba.protocol.event.CoordinationContextCloseEvent;
import net.jotorren.microservices.rtsba.protocol.event.CoordinationContextCompensateEvent;
import net.jotorren.microservices.rtsba.protocol.event.CoordinationContextOpenEvent;
import net.jotorren.microservices.rtsba.protocol.event.CoordinationContextPartialEvent;
import net.jotorren.microservices.rtsba.protocol.event.CoordinationContextRegistrationEvent;
import net.jotorren.microservices.rtsba.protocol.event.CoordinationContextResumeEvent;
import net.jotorren.microservices.rtsba.protocol.event.CoordinationContextSuspendEvent;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class CoordinationContextProtocol {
	
	private transient static final Logger LOG = LoggerFactory.getLogger(CoordinationContextProtocol.class);
	
	@Autowired
	private RtsBaProperties configuration;
	
	@Autowired
	private EventBus sagaBus;
	
	@Autowired
	private RedisMessageListenerContainer redis;
	
	private CountDownLatch openSignal = new CountDownLatch(1);
	private CountDownLatch activeSignal = new CountDownLatch(1);
	
	public String open(long timeout){
		String coordCtxId = UUID.randomUUID().toString();
		
		GenericDomainEventMessage<CoordinationContextOpenEvent> msg = 
				new GenericDomainEventMessage<>("rts-open", coordCtxId, 0, new CoordinationContextOpenEvent(coordCtxId, timeout));
		sagaBus.publish(msg);

		// TODO Consider moving the response queue from REDIS to RabbitMQ
		MessageListenerAdapter lsnr = new MessageListenerAdapter(this, "handleOpen");
		lsnr.afterPropertiesSet();
		redis.addMessageListener(lsnr, new ChannelTopic(coordCtxId));
		
		try {
			LOG.info("RTS-BA PROTOCOL :: Waiting (max {}) for coordination context {} open", configuration.getOpenTimeout(), coordCtxId);
			if (!openSignal.await(configuration.getOpenTimeout(), TimeUnit.MILLISECONDS)){
				LOG.error("RTS-BA PROTOCOL :: Coordination context {} not opened before timeout", coordCtxId);
				throw new RtsBaException("RTS-BA-AOP-21", "Unable to open coordination context "+coordCtxId, 
						new TransactionTimedOutException("No open response received"));
			}
		} catch (InterruptedException e) {
			LOG.error("RTS-BA PROTOCOL :: Coordination context {} - {}", coordCtxId, e.getMessage());
			throw new RtsBaException("RTS-BA-AOP-22", "Unable to open coordination context "+coordCtxId, 
					new TransactionTimedOutException("Open await interrupted"));
		}
		
		openSignal = new CountDownLatch(1);
		LOG.info("RTS-BA PROTOCOL :: Coordination context {} open", coordCtxId);
		return coordCtxId;
	}

	public String register(String coordCtxId, long coordCtxSequence, CoordinationContextParticipant participant){
		CoordinationContextRegistrationEvent event = new CoordinationContextRegistrationEvent(coordCtxId, participant);
		
		GenericDomainEventMessage<CoordinationContextRegistrationEvent> msg = 
				new GenericDomainEventMessage<>("rts-registration", coordCtxId, coordCtxSequence, event);
		sagaBus.publish(msg);
		
		// TODO Consider moving the response queue from REDIS to RabbitMQ
		MessageListenerAdapter lsnr = new MessageListenerAdapter(this, "handleActivation");
		lsnr.afterPropertiesSet();
		redis.addMessageListener(lsnr, new ChannelTopic(coordCtxId + "-" + event.getActivityId()));
		
		try {
			LOG.info("RTS-BA PROTOCOL :: Waiting (max {}) for coordination context {} :: Activity {} activation", 
					configuration.getActivationTimeout(), coordCtxId, event.getActivityId());
			if (!activeSignal.await(configuration.getActivationTimeout(), TimeUnit.MILLISECONDS)){
				LOG.error("RTS-BA PROTOCOL :: Coordination context {} :: Activity {} - no activation received before timeout", coordCtxId, event.getActivityId());
				throw new RtsBaException("RTS-BA-AOP-31", "Unable to register activity inside coordination context "+coordCtxId, 
						new TransactionTimedOutException("No activation received"));
			}
		} catch (InterruptedException e) {
			LOG.error("RTS-BA PROTOCOL Coordination context {} :: Activity {} - {}", coordCtxId, event.getActivityId(), e.getMessage());
			throw new RtsBaException("RTS-BA-AOP-32", "Unable to register activity inside coordination context "+coordCtxId, 
					new TransactionTimedOutException("Activation await interrupted"));
		}

		// Removing the listener causes an error on next redis requests
		activeSignal = new CountDownLatch(1);
		LOG.info("RTS-BA PROTOCOL :: Coordination context {} :: Activity {} active", coordCtxId, event.getActivityId());
		return event.getActivityId();
	}

	// TODO The message should be a typed object with its own properties
    public void handleOpen(String message) {
    	LOG.info("RTS-BA PROTOCOL :: Received <{}>", message);
    	openSignal.countDown();
    }
    
	// TODO The message should be a typed object with its own properties
    public void handleActivation(String message) {
    	LOG.info("RTS-BA PROTOCOL :: Received <{}>", message);
    	activeSignal.countDown();
    }
    
	public void partial(String coordCtxId, long coordCtxSequence, String partialId){
		GenericDomainEventMessage<CoordinationContextPartialEvent> msg = 
				new GenericDomainEventMessage<>("rts-partial", coordCtxId, coordCtxSequence, 
						new CoordinationContextPartialEvent(coordCtxId, partialId));
		sagaBus.publish(GenericDomainEventMessage.asEventMessage(msg));
	}

	public void suspend(String coordCtxId, long coordCtxSequence){
		GenericDomainEventMessage<CoordinationContextSuspendEvent> msg = 
				new GenericDomainEventMessage<>("rts-suspend", coordCtxId, coordCtxSequence, 
						new CoordinationContextSuspendEvent(coordCtxId));
		sagaBus.publish(GenericDomainEventMessage.asEventMessage(msg));
	}
	
	public void resume(String coordCtxId, long coordCtxSequence){
		GenericDomainEventMessage<CoordinationContextResumeEvent> msg = 
				new GenericDomainEventMessage<>("rts-resume", coordCtxId, coordCtxSequence, 
						new CoordinationContextResumeEvent(coordCtxId));
		sagaBus.publish(GenericDomainEventMessage.asEventMessage(msg));
	}
	
	public void cancel(String coordCtxId, long coordCtxSequence){
		GenericDomainEventMessage<CoordinationContextCancelEvent> msg = 
				new GenericDomainEventMessage<>("rts-cancel", coordCtxId, coordCtxSequence, 
						new CoordinationContextCancelEvent(coordCtxId));
		sagaBus.publish(GenericDomainEventMessage.asEventMessage(msg));
	}
	
	public void close(String coordCtxId, long coordCtxSequence){
		GenericDomainEventMessage<CoordinationContextCloseEvent> msg = 
				new GenericDomainEventMessage<>("rts-close", coordCtxId, coordCtxSequence, 
						new CoordinationContextCloseEvent(coordCtxId));
		sagaBus.publish(GenericDomainEventMessage.asEventMessage(msg));
	}

	public void compensate(String coordCtxId, long coordCtxSequence){
		GenericDomainEventMessage<CoordinationContextCompensateEvent> msg = 
				new GenericDomainEventMessage<>("rts-compensate", coordCtxId, coordCtxSequence, 
						new CoordinationContextCompensateEvent(coordCtxId));
		sagaBus.publish(GenericDomainEventMessage.asEventMessage(msg));
	}
}
