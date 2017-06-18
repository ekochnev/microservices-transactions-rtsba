package net.jotorren.microservices.rtsba.protocol.event;

public class CoordinationContextCompensateEvent extends CoordinationContextEvent {
	private static final long serialVersionUID = -8960446376732372803L;

	public CoordinationContextCompensateEvent(String transactionIdentifier) {
		super(transactionIdentifier);
	}
}
