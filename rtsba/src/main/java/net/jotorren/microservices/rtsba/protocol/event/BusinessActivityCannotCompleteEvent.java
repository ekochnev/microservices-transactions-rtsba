package net.jotorren.microservices.rtsba.protocol.event;

public class BusinessActivityCannotCompleteEvent extends BusinessActivityEvent {
	private static final long serialVersionUID = -830547687915910027L;

	public BusinessActivityCannotCompleteEvent(String coordinationContextId, String activityId) {
		super(coordinationContextId, activityId);
	}
}
