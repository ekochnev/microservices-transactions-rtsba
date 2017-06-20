package net.jotorren.microservices.rtsba.protocol.event;

import java.io.Serializable;

public abstract class BusinessActivityEvent implements Serializable {
	private static final long serialVersionUID = 6519008042493031966L;

	private String coordinationContextId;
	private String activityId;

	public BusinessActivityEvent(String coordinationContextId, String activityId) {
		super();
		this.coordinationContextId = coordinationContextId;
		this.activityId = activityId;
	}

	public String getCoordinationContextId() {
		return this.coordinationContextId;
	}
	
	public String getActivityId() {
		return this.activityId;
	}
	
	public String getActivityInstanceId() {
		return this.coordinationContextId + "-" + this.activityId;
	}
}
