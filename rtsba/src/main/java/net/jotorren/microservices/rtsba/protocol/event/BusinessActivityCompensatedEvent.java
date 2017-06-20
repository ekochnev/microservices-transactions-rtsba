package net.jotorren.microservices.rtsba.protocol.event;

public class BusinessActivityCompensatedEvent extends BusinessActivityEvent {
	private static final long serialVersionUID = 3763616971699322457L;

	public BusinessActivityCompensatedEvent(String coordinationContextId, String activityId) {
		super(coordinationContextId, activityId);
	}
}
