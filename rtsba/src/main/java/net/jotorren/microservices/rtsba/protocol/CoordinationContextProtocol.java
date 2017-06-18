package net.jotorren.microservices.rtsba.protocol;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.transaction.TransactionalException;

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
	private EventBus sagaBus;
	
	@Autowired
	private RedisMessageListenerContainer redis;
	
	private CountDownLatch activeSignal = new CountDownLatch(1);
	
	public String open(long timeout){
		String txId = UUID.randomUUID().toString();
		
		GenericDomainEventMessage<CoordinationContextOpenEvent> msg = 
				new GenericDomainEventMessage<>("rts-open", txId, 0, new CoordinationContextOpenEvent(txId, timeout));
		sagaBus.publish(msg);
		
		return txId;
	}

	public String register(String txId, long txSeqNumber, CoordinationContextParticipant participant){
		CoordinationContextRegistrationEvent event = new CoordinationContextRegistrationEvent(txId, participant);
		
		GenericDomainEventMessage<CoordinationContextRegistrationEvent> msg = 
				new GenericDomainEventMessage<>("rts-registration", txId, txSeqNumber, event);
		sagaBus.publish(msg);
		
		// TODO Consider moving the response queue from REDIS to RabbitMQ
		MessageListenerAdapter lsnr = new MessageListenerAdapter(this, "handle");
		lsnr.afterPropertiesSet();
		redis.addMessageListener(lsnr, new ChannelTopic(txId + "-" + event.getActivityId()));
		
		try {
			if (!activeSignal.await(participant.getActivationTimeout(), TimeUnit.MILLISECONDS)){
				LOG.error("RTS-BA PROTOCOL :: Transaction {} :: Activity {} - no activation received before timeout", txId, event.getActivityId());
				throw new TransactionalException("RTS-BA PROTOCOL :: Unable to register activity inside TX "+txId, 
						new TransactionTimedOutException("RTS-BA PROTOCOL :: No activation received"));
			}
		} catch (InterruptedException e) {
			LOG.error("RTS-BA PROTOCOL Transaction {} :: Activity {} - {}", txId, event.getActivityId(), e.getMessage());
			throw new TransactionalException("RTS-BA PROTOCOL :: Unable to register activity inside TX "+txId, 
					new TransactionTimedOutException("RTS-BA PROTOCOL :: Activation await interrupted"));
		}

		// Removing the listener causes an error on next redis requests
		return event.getActivityId();
	}

	// TODO The message should be a typed object with its own properties
    public void handle(String message) {
    	LOG.info("RTS-BA PROTOCOL :: Received <{}>", message);
    	activeSignal.countDown();
    }
    
	public void partial(String txId, long txSeqNumber, String partialId){
		GenericDomainEventMessage<CoordinationContextPartialEvent> msg = 
				new GenericDomainEventMessage<>("rts-partial", txId, txSeqNumber, new CoordinationContextPartialEvent(txId, partialId));
		sagaBus.publish(GenericDomainEventMessage.asEventMessage(msg));
	}

	public void suspend(String txId, long txSeqNumber){
		GenericDomainEventMessage<CoordinationContextSuspendEvent> msg = 
				new GenericDomainEventMessage<>("rts-suspend", txId, txSeqNumber, new CoordinationContextSuspendEvent(txId));
		sagaBus.publish(GenericDomainEventMessage.asEventMessage(msg));
	}
	
	public void resume(String txId, long txSeqNumber){
		GenericDomainEventMessage<CoordinationContextResumeEvent> msg = 
				new GenericDomainEventMessage<>("rts-resume", txId, txSeqNumber, new CoordinationContextResumeEvent(txId));
		sagaBus.publish(GenericDomainEventMessage.asEventMessage(msg));
	}
	
	public void cancel(String txId, long txSeqNumber){
		GenericDomainEventMessage<CoordinationContextCancelEvent> msg = 
				new GenericDomainEventMessage<>("rts-cancel", txId, txSeqNumber, new CoordinationContextCancelEvent(txId));
		sagaBus.publish(GenericDomainEventMessage.asEventMessage(msg));
	}
	
	public void close(String txId, long txSeqNumber){
		GenericDomainEventMessage<CoordinationContextCloseEvent> msg = 
				new GenericDomainEventMessage<>("rts-close", txId, txSeqNumber, new CoordinationContextCloseEvent(txId));
		sagaBus.publish(GenericDomainEventMessage.asEventMessage(msg));
	}

	public void compensate(String txId, long txSeqNumber){
		GenericDomainEventMessage<CoordinationContextCompensateEvent> msg = 
				new GenericDomainEventMessage<>("rts-compensate", txId, txSeqNumber, new CoordinationContextCompensateEvent(txId));
		sagaBus.publish(GenericDomainEventMessage.asEventMessage(msg));
	}
}
