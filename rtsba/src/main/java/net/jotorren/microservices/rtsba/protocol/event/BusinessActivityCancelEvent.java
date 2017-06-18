package net.jotorren.microservices.rtsba.protocol.event;

public class BusinessActivityCancelEvent extends BusinessActivityEvent {
	private static final long serialVersionUID = 5556054659398809450L;

	public BusinessActivityCancelEvent(String compositeTransactionId, String activityId) {
		super(compositeTransactionId, activityId);
	}
}
