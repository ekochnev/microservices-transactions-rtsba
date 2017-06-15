package net.jotorren.microservices.rtsba.protocol.event;

public class CoordinationContextTimeoutEvent extends CoordinationContextEvent {
	private static final long serialVersionUID = -8246445722771088444L;

	private long timeout;
	
	public CoordinationContextTimeoutEvent(String transactionIdentifier, long timeout) {
		super(transactionIdentifier);
		this.timeout = timeout;
	}

	public long getTimeout() {
		return timeout;
	}
}
