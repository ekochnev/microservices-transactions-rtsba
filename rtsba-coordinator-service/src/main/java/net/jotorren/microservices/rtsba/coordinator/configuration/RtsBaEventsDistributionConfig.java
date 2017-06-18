package net.jotorren.microservices.rtsba.coordinator.configuration;

import org.axonframework.amqp.eventhandling.RoutingKeyResolver;
import org.axonframework.amqp.eventhandling.spring.SpringAMQPMessageSource;
import org.axonframework.amqp.eventhandling.spring.SpringAMQPPublisher;
import org.axonframework.eventhandling.EventBus;
import org.axonframework.eventhandling.EventProcessor;
import org.axonframework.eventhandling.SubscribingEventProcessor;
import org.axonframework.eventhandling.saga.AbstractSagaManager;
import org.axonframework.serialization.JavaSerializer;
import org.axonframework.serialization.Serializer;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.rabbitmq.client.Channel;

import net.jotorren.microservices.rtsba.coordinator.saga.BusinessActivitySaga;
import net.jotorren.microservices.rtsba.coordinator.saga.CoordinationContextSaga;
import net.jotorren.microservices.rtsba.protocol.amqp.RoutingKeyPropertyResolver;

@Configuration
public class RtsBaEventsDistributionConfig {

	public static final String QUEUE_NAME = "Axon.EventQueue";
	
	@Value("${spring.amqp.exchange.name:exchange_name}")
	private String exchangeName;

	@Value("${spring.amqp.routing.key:routing_key}")
	private String routingKey;
	
	@Value("${spring.amqp.queue.durable:false}")
	private boolean isDurableQueue;

    @Bean
    Queue queue() {
        return new Queue(QUEUE_NAME, isDurableQueue);
    }
    
    @Bean
    TopicExchange exchange() {
        return new TopicExchange(exchangeName);
    }

    @Bean
    Binding binding(Queue queue, TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(routingKey);
    }
    
	@Bean
	public Serializer serializer(){
		return new JavaSerializer();
	}
	
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
	public EventProcessor txSagaEventProcessor(AbstractSagaManager<CoordinationContextSaga> sagaManager, SpringAMQPMessageSource sagaRemoteBus) {
		SubscribingEventProcessor eventProcessor = 
				new SubscribingEventProcessor("txSagaEventProcessor", sagaManager, sagaRemoteBus);
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
	
	@Bean
	public RoutingKeyResolver routingKeyPropertyResolver(){
		return new RoutingKeyPropertyResolver(routingKey);
	}
	
	@Bean
	public SpringAMQPPublisher sagaEventPublisher(EventBus sagaLocalBus, RoutingKeyResolver routingKeyPropertyResolver){
		SpringAMQPPublisher pub = new SpringAMQPPublisher(sagaLocalBus);
		pub.setRoutingKeyResolver(routingKeyPropertyResolver);
		pub.start();
		
		return pub; 
	}
}
