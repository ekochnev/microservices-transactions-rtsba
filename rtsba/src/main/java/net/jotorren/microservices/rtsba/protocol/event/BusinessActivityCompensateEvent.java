package net.jotorren.microservices.rtsba.protocol.event;

public class BusinessActivityCompensateEvent extends BusinessActivityEvent {
	private static final long serialVersionUID = 2399572890515986815L;

	public BusinessActivityCompensateEvent(String coordinationContextId, String activityId) {
		super(coordinationContextId, activityId);
	}
}
