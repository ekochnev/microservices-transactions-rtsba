package net.jotorren.microservices.rtsba.coordinator.configuration;

import org.axonframework.amqp.eventhandling.spring.SpringAMQPMessageSource;
import org.axonframework.eventhandling.EventProcessor;
import org.axonframework.eventhandling.SubscribingEventProcessor;
import org.axonframework.eventhandling.saga.AbstractSagaManager;
import org.axonframework.serialization.Serializer;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.rabbitmq.client.Channel;

import net.jotorren.microservices.rtsba.coordinator.saga.BusinessActivitySaga;
import net.jotorren.microservices.rtsba.coordinator.saga.CoordinationContextSaga;

@Configuration
public class RtsBaEventsDistributionConfig {
	public static final String QUEUE_NAME = "Axon.EventQueue";

    @Bean
    public SpringAMQPMessageSource sagaRemoteBus(Serializer serializer) {
        return new SpringAMQPMessageSource(serializer) {
            @RabbitListener(queues = QUEUE_NAME)
            @Override
            public void onMessage(Message message, Channel channel) throws Exception {
                super.onMessage(message, channel);
            }
        };
    }

	@Bean
	public EventProcessor ctxSagaEventProcessor(AbstractSagaManager<CoordinationContextSaga> sagaManager, SpringAMQPMessageSource sagaRemoteBus) {
		SubscribingEventProcessor eventProcessor = 
				new SubscribingEventProcessor("ctxSagaEventProcessor", sagaManager, sagaRemoteBus);
		eventProcessor.start();

		return eventProcessor;
	}

	@Bean
	public EventProcessor actSagaEventProcessor(AbstractSagaManager<BusinessActivitySaga> sagaManager, SpringAMQPMessageSource sagaRemoteBus) {
		SubscribingEventProcessor eventProcessor = 
				new SubscribingEventProcessor("actSagaEventProcessor", sagaManager, sagaRemoteBus);
		eventProcessor.start();

		return eventProcessor;
	}
}
