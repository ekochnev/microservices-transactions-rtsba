package net.jotorren.microservices.rtsba.coordinator.saga;

import java.util.Set;

import org.axonframework.eventhandling.saga.AssociationValue;
import org.axonframework.eventhandling.saga.repository.jdbc.JdbcSagaStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.jotorren.microservices.rtsba.protocol.BusinessActivityStatus;

@Service
public class CoordinatorSagaService {

	@Autowired
	private JdbcSagaStore sagaStore;

	public boolean isCoordinationContextClosed(String coordinationContextId) {
		AssociationValue association = new AssociationValue(CoordinationContextSaga.ASSOCIATION_PROPERTY, coordinationContextId);
		Set<String> items = sagaStore.findSagas(CoordinationContextSaga.class, association);
		if (items.isEmpty()) {
			return true;
		}
		
		String sagaId = items.iterator().next();
		CoordinationContextSaga saga = this.sagaStore.loadSaga(CoordinationContextSaga.class, sagaId).saga();
		if (null == saga) {
			return true;
		}
		
		return saga.isExpired();
	}
	
	public BusinessActivityStatus getBusinessActivityStatus(String activityInstanceId) {
		AssociationValue association = new AssociationValue(BusinessActivitySaga.ASSOCIATION_PROPERTY, activityInstanceId);
		Set<String> items = sagaStore.findSagas(BusinessActivitySaga.class, association);
		if (items.isEmpty()) {
			return null;
		}
		
		String sagaId = items.iterator().next();
		BusinessActivitySaga saga = this.sagaStore.loadSaga(BusinessActivitySaga.class, sagaId).saga();
		if (null == saga) {
			return null;
		}
		
		return saga.getStatus();
	}
}
