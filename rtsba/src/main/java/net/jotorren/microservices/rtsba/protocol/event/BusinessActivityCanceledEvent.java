package net.jotorren.microservices.rtsba.protocol.event;

public class BusinessActivityCanceledEvent extends BusinessActivityEvent {
	private static final long serialVersionUID = 2048122676816567837L;

	public BusinessActivityCanceledEvent(String coordinationContextId, String activityId) {
		super(coordinationContextId, activityId);
	}
}
