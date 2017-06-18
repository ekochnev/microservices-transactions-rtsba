package net.jotorren.microservices.rtsba.protocol.event;

public class BusinessActivityFailCompensatingEvent extends BusinessActivityEvent {
	private static final long serialVersionUID = -2700777982934567848L;

	public BusinessActivityFailCompensatingEvent(String compositeTransactionId, String activityId) {
		super(compositeTransactionId, activityId);
	}
}
