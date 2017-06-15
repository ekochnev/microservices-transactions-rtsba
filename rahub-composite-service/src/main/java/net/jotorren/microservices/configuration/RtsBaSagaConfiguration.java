package net.jotorren.microservices.configuration;

import java.sql.SQLException;

import javax.sql.DataSource;

import org.axonframework.common.jdbc.ConnectionProvider;
import org.axonframework.common.transaction.NoTransactionManager;
import org.axonframework.common.transaction.TransactionManager;
import org.axonframework.eventhandling.EventBus;
import org.axonframework.eventhandling.saga.AbstractSagaManager;
import org.axonframework.eventhandling.saga.AnnotatedSagaManager;
import org.axonframework.eventhandling.saga.ResourceInjector;
import org.axonframework.eventhandling.saga.SagaRepository;
import org.axonframework.eventhandling.saga.repository.AnnotatedSagaRepository;
import org.axonframework.eventhandling.saga.repository.SagaStore;
import org.axonframework.eventhandling.saga.repository.jdbc.HsqlSagaSqlSchema;
import org.axonframework.eventhandling.saga.repository.jdbc.JdbcSagaStore;
import org.axonframework.eventhandling.saga.repository.jdbc.SagaSqlSchema;
import org.axonframework.eventhandling.scheduling.EventScheduler;
import org.axonframework.eventhandling.scheduling.quartz.QuartzEventScheduler;
import org.axonframework.eventsourcing.eventstore.EmbeddedEventStore;
import org.axonframework.eventsourcing.eventstore.EventStorageEngine;
import org.axonframework.eventsourcing.eventstore.jdbc.EventTableFactory;
import org.axonframework.eventsourcing.eventstore.jdbc.HsqlEventTableFactory;
import org.axonframework.eventsourcing.eventstore.jdbc.JdbcEventStorageEngine;
import org.axonframework.spring.config.AnnotationDriven;
import org.axonframework.spring.jdbc.SpringDataSourceConnectionProvider;
import org.axonframework.spring.saga.SpringResourceInjector;
import org.quartz.Scheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import net.jotorren.microservices.rtsba.coordinator.BusinessActivitySaga;
import net.jotorren.microservices.rtsba.coordinator.CoordinationContextSaga;

@AnnotationDriven
@Configuration
public class RtsBaSagaConfiguration {

	@Value("${spring.jpa.generate-ddl:true}")
	private boolean generateDDL;

	@Autowired
	private DataSource dataSource;
	
	// -------------------------------------------------------------------	
	
	
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

	@Bean
	public SagaSqlSchema sagaSqlSchema() {
		return new HsqlSagaSqlSchema();
	}

	@Bean
	public JdbcSagaStore sagaStore(SagaSqlSchema sagaSqlSchema) {
		JdbcSagaStore jdbc = new JdbcSagaStore(dataSource, sagaSqlSchema);
		if (generateDDL) {
			try {
				jdbc.createSchema();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return jdbc;
	}

	@Bean
	public SagaRepository<CoordinationContextSaga> txSagaRepository(SagaStore<Object> sagaStore, ResourceInjector resourceInjector) {
		return new AnnotatedSagaRepository<>(CoordinationContextSaga.class, sagaStore, resourceInjector);
	}

	@Bean
	public SagaRepository<BusinessActivitySaga> actSagaRepository(SagaStore<Object> sagaStore, ResourceInjector resourceInjector) {
		return new AnnotatedSagaRepository<>(BusinessActivitySaga.class, sagaStore, resourceInjector);
	}
	
	@Bean
	public AbstractSagaManager<CoordinationContextSaga> txSagaManager(SagaRepository<CoordinationContextSaga> sagaRepository) {
		return new AnnotatedSagaManager<>(CoordinationContextSaga.class, sagaRepository);
	}

	@Bean
	public AbstractSagaManager<BusinessActivitySaga> actSagaManager(SagaRepository<BusinessActivitySaga> sagaRepository) {
		return new AnnotatedSagaManager<>(BusinessActivitySaga.class, sagaRepository);
	}
	
	// -------------------------------------------------------------------
	
	@Bean
	public EventScheduler eventScheduler(Scheduler scheduler, EventBus sagaLocalBus, TransactionManager eventTransactionManager){
		QuartzEventScheduler qs = new QuartzEventScheduler();
		qs.setScheduler(scheduler);
		qs.setEventBus(sagaLocalBus);
		qs.setTransactionManager(eventTransactionManager);
		return qs;
	}
}
