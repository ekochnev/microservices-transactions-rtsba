package net.jotorren.microservices.rtsba.protocol.event;

public class BusinessActivityExitedEvent extends BusinessActivityEvent {
	private static final long serialVersionUID = 2040583473008430921L;

	public BusinessActivityExitedEvent(String coordinationContextId, String activityId) {
		super(coordinationContextId, activityId);
	}
}
