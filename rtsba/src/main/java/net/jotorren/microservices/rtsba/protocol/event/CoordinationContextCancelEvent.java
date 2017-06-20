package net.jotorren.microservices.rtsba.protocol.event;

public class CoordinationContextCancelEvent extends CoordinationContextEvent {
	private static final long serialVersionUID = 6099461420983172605L;

	public CoordinationContextCancelEvent(String coordinationContextId) {
		super(coordinationContextId);
	}
}
