package net.jotorren.microservices.rtsba.protocol.event;

public class BusinessActivityCompletedEvent extends BusinessActivityEvent {
	private static final long serialVersionUID = 323522132908308956L;

	public BusinessActivityCompletedEvent(String compositeTransactionId, String activityId) {
		super(compositeTransactionId, activityId);
	}
}
