package net.jotorren.microservices.rtsba.protocol.event;

public class CoordinationContextResumeEvent extends CoordinationContextEvent {
	private static final long serialVersionUID = 901006075168195988L;

	public CoordinationContextResumeEvent(String coordinationContextId) {
		super(coordinationContextId);
	}
}
