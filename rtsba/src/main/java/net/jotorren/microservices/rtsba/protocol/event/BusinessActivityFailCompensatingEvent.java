package net.jotorren.microservices.rtsba.protocol.event;

public class BusinessActivityFailCompensatingEvent extends BusinessActivityEvent {
	private static final long serialVersionUID = -2700777982934567848L;

	public BusinessActivityFailCompensatingEvent(String coordinationContextId, String activityId) {
		super(coordinationContextId, activityId);
	}
}
