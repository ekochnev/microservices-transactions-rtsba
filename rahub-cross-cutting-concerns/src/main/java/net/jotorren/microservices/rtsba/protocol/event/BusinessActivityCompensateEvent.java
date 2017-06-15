package net.jotorren.microservices.rtsba.protocol.event;

public class BusinessActivityCompensateEvent extends BusinessActivityEvent {
	private static final long serialVersionUID = 2399572890515986815L;

	public BusinessActivityCompensateEvent(String compositeTransactionId, String activityId) {
		super(compositeTransactionId, activityId);
	}
}
