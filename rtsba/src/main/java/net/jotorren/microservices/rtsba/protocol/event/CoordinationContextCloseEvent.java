package net.jotorren.microservices.rtsba.protocol.event;

public class CoordinationContextCloseEvent extends CoordinationContextEvent {
	private static final long serialVersionUID = -6278707713449616022L;

	public CoordinationContextCloseEvent(String coordinationContextId) {
		super(coordinationContextId);
	}
}
