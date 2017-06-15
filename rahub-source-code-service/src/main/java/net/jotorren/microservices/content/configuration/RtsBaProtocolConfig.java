package net.jotorren.microservices.content.configuration;

import javax.sql.DataSource;

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
import org.axonframework.spring.jdbc.SpringDataSourceConnectionProvider;
import org.axonframework.spring.saga.SpringResourceInjector;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import net.jotorren.microservices.rtsba.protocol.amqp.RoutingKeyPropertyResolver;

@Configuration
public class RtsBaProtocolConfig {
	public static final String QUEUE_NAME = "Axon.EventQueue";
	
	@Value("${spring.jpa.generate-ddl:true}")
	private boolean generateDDL;
	
	@Value("${spring.amqp.exchange.name:exchange_name}")
	private String exchangeName;

	@Value("${spring.amqp.routing.key:routing_key}")
	private String routingKey;
	
	@Value("${spring.amqp.queue.durable:false}")
	private boolean isDurableQueue;

	@Autowired
	private DataSource dataSource;

	// Local BUS and Events storage
	@Bean
	public ResourceInjector resourceInjector() {
		return new SpringResourceInjector();
	}

    @Bean
    public ConnectionProvider eventConnectionProvider() {
        return new SpringDataSourceConnectionProvider(dataSource);
    }

    @Bean
    public TransactionManager eventTransactionManager(){
    	return NoTransactionManager.INSTANCE;
    }

    @Bean
    public EventTableFactory eventSchemaFactory() {
        return HsqlEventTableFactory.INSTANCE;
    }
    
    @Bean
    public EventStorageEngine eventStorageEngine() {
    	JdbcEventStorageEngine jdbc = new JdbcEventStorageEngine(eventConnectionProvider(), eventTransactionManager());
		if (generateDDL) {
			jdbc.createSchema(eventSchemaFactory());
		}
        return jdbc;
    }

    @Bean
    public EventBus sagaLocalBus() {
        return new EmbeddedEventStore(eventStorageEngine());
    }
    
    // Remote BUS (RabbitMQ)
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
