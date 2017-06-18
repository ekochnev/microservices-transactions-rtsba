package net.jotorren.microservices.rtsba.autoconfigure;

import org.axonframework.amqp.eventhandling.RoutingKeyResolver;
import org.axonframework.amqp.eventhandling.spring.SpringAMQPPublisher;
import org.axonframework.common.jdbc.ConnectionProvider;
import org.axonframework.common.transaction.NoTransactionManager;
import org.axonframework.common.transaction.TransactionManager;
import org.axonframework.eventhandling.EventBus;
import org.axonframework.eventhandling.saga.ResourceInjector;
import org.axonframework.eventsourcing.eventstore.EmbeddedEventStore;
import org.axonframework.eventsourcing.eventstore.EventStorageEngine;
import org.axonframework.eventsourcing.eventstore.jdbc.EventTableFactory;
import org.axonframework.eventsourcing.eventstore.jdbc.HsqlEventTableFactory;
import org.axonframework.eventsourcing.eventstore.jdbc.JdbcEventStorageEngine;
import org.axonframework.serialization.JavaSerializer;
import org.axonframework.serialization.Serializer;
import org.axonframework.spring.saga.SpringResourceInjector;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import net.jotorren.microservices.rtsba.RtsBaProtocolProperties;
import net.jotorren.microservices.rtsba.participant.RtsBaTransactional;
import net.jotorren.microservices.rtsba.protocol.amqp.RoutingKeyPropertyResolver;

@Configuration
@ConditionalOnClass(RtsBaTransactional.class)
@EnableConfigurationProperties(RtsBaProtocolProperties.class)
public class RtsBaProtocolAutoConfiguration {
	public static final String QUEUE_NAME = "Axon.EventQueue";
	
	@Autowired
	private RtsBaProtocolProperties configuration;

	// Local BUS and Events storage
	@Bean
	@ConditionalOnMissingBean
	public ResourceInjector resourceInjector() {
		return new SpringResourceInjector();
	}

    @Bean
    @ConditionalOnMissingBean
    public TransactionManager eventTransactionManager(){
    	return NoTransactionManager.INSTANCE;
    }

    @Bean
    @ConditionalOnMissingBean
    public EventTableFactory eventSchemaFactory() {
        return HsqlEventTableFactory.INSTANCE;
    }
    
    @Bean
    @ConditionalOnMissingBean
    public EventStorageEngine eventStorageEngine(ConnectionProvider eventConnectionProvider) {
    	JdbcEventStorageEngine jdbc = new JdbcEventStorageEngine(eventConnectionProvider, eventTransactionManager());
		if (configuration.isGenerateDDL()) {
			jdbc.createSchema(eventSchemaFactory());
		}
        return jdbc;
    }

    @Bean
    @ConditionalOnMissingBean
    public EventBus sagaLocalBus(EventStorageEngine eventStorageEngine) {
        return new EmbeddedEventStore(eventStorageEngine);
    }
    
    // Remote BUS (RabbitMQ)
    @Bean
    @ConditionalOnMissingBean
    Queue queue() {
        return new Queue(QUEUE_NAME, configuration.isDurableQueue());
    }
    
    @Bean
    @ConditionalOnMissingBean
    TopicExchange exchange() {
        return new TopicExchange(configuration.getExchangeName());
    }

    @Bean
    @ConditionalOnMissingBean
    Binding binding(Queue queue, TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(configuration.getRoutingKey());
    }

	@Bean
	@ConditionalOnMissingBean
	public Serializer serializer(){
		return new JavaSerializer();
	}
	
	@Bean
	@ConditionalOnMissingBean
	public RoutingKeyResolver routingKeyPropertyResolver(){
		return new RoutingKeyPropertyResolver(configuration.getRoutingKey());
	}
	
	@Bean
	@ConditionalOnMissingBean
	public SpringAMQPPublisher sagaEventPublisher(EventBus sagaLocalBus, RoutingKeyResolver routingKeyPropertyResolver){
		SpringAMQPPublisher pub = new SpringAMQPPublisher(sagaLocalBus);
		pub.setRoutingKeyResolver(routingKeyPropertyResolver);
		pub.start();
		
		return pub; 
	}
}
