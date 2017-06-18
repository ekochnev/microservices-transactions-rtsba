package net.jotorren.microservices.rtsba.protocol.event;

public class CoordinationContextSuspendEvent extends CoordinationContextEvent {
	private static final long serialVersionUID = 4238064203141145301L;

	public CoordinationContextSuspendEvent(String transactionIdentifier) {
		super(transactionIdentifier);
	}
}
