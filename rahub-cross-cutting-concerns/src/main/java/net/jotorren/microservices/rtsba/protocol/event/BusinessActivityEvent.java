package net.jotorren.microservices.rtsba.protocol.event;

import java.io.Serializable;

public abstract class BusinessActivityEvent implements Serializable {
	private static final long serialVersionUID = 6519008042493031966L;

	private String compositeTransactionId;
	private String activityId;

	public BusinessActivityEvent(String compositeTransactionId, String activityId) {
		super();
		this.compositeTransactionId = compositeTransactionId;
		this.activityId = activityId;
	}

	public String getCompositeTransactionId() {
		return compositeTransactionId;
	}
	
	public String getActivityId() {
		return activityId;
	}
	
	public String getActivityInstanceId() {
		return compositeTransactionId + "-" + activityId;
	}
}
