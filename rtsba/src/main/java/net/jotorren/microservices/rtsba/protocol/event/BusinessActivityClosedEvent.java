package net.jotorren.microservices.rtsba.protocol.event;

public class BusinessActivityClosedEvent extends BusinessActivityEvent {
	private static final long serialVersionUID = 4293517972186199786L;

	public BusinessActivityClosedEvent(String compositeTransactionId, String activityId) {
		super(compositeTransactionId, activityId);
	}
}
