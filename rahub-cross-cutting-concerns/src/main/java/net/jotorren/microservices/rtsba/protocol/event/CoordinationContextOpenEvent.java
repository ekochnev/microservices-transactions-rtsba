package net.jotorren.microservices.rtsba.protocol.event;

public class CoordinationContextOpenEvent extends CoordinationContextEvent {
	private static final long serialVersionUID = 7209733646927073549L;
	
	private long timeout = -1;
	
	public CoordinationContextOpenEvent(String compositeTransactionId, long timeout) {
		super(compositeTransactionId);
		this.timeout = timeout;
	}

	public long getTimeout() {
		return timeout;
	}
	
}
