package net.jotorren.microservices.rtsba.coordinator.configuration;

import java.sql.SQLException;

import javax.sql.DataSource;

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
import org.axonframework.spring.config.AnnotationDriven;
import org.quartz.Scheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import net.jotorren.microservices.rtsba.RtsBaProtocolProperties;
import net.jotorren.microservices.rtsba.coordinator.saga.BusinessActivitySaga;
import net.jotorren.microservices.rtsba.coordinator.saga.CoordinationContextSaga;

@AnnotationDriven
@Configuration
public class RtsBaSagaConfiguration {

	@Autowired
	private RtsBaProtocolProperties configuration;
	
	@Autowired
	private DataSource dataSource;
	
	@Bean
	public SagaSqlSchema sagaSqlSchema() {
		return new HsqlSagaSqlSchema();
	}

	@Bean
	public JdbcSagaStore sagaStore(SagaSqlSchema sagaSqlSchema) {
		JdbcSagaStore jdbc = new JdbcSagaStore(dataSource, sagaSqlSchema);
		if (configuration.isGenerateDDL()) {
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
