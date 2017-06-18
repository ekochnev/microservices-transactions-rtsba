package net.jotorren.microservices.rtsba.protocol.event;

public class BusinessActivityNotCompletedEvent extends BusinessActivityEvent {
	private static final long serialVersionUID = -3054349566644475222L;

	public BusinessActivityNotCompletedEvent(String compositeTransactionId, String activityId) {
		super(compositeTransactionId, activityId);
	}
}
