package net.jotorren.microservices.rtsba.protocol.event;

public class BusinessActivityFailedEvent extends BusinessActivityEvent {
	private static final long serialVersionUID = -665111633544358943L;

	public BusinessActivityFailedEvent(String coordinationContextId, String activityId) {
		super(coordinationContextId, activityId);
	}
}
