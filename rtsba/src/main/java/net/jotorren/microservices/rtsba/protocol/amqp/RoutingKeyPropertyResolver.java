package net.jotorren.microservices.rtsba.protocol.amqp;

import org.axonframework.amqp.eventhandling.RoutingKeyResolver;
import org.axonframework.eventhandling.EventMessage;

public class RoutingKeyPropertyResolver implements RoutingKeyResolver {

	private String value;
	
	public RoutingKeyPropertyResolver(String value){
		super();
		this.value = value;
	}
	
	@Override
	public String resolveRoutingKey(EventMessage<?> eventMessage) {
		return value;
	}

}
