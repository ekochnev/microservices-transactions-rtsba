package net.jotorren.microservices.rtsba.protocol.event;

public class BusinessActivityFailEvent extends BusinessActivityEvent {
	private static final long serialVersionUID = 7782367504555599676L;

	public BusinessActivityFailEvent(String compositeTransactionId, String activityId) {
		super(compositeTransactionId, activityId);
	}
}
