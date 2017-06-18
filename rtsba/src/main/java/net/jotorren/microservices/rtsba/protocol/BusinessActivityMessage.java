package net.jotorren.microservices.rtsba.protocol;

import java.io.Serializable;

public class BusinessActivityMessage implements Serializable {
	private static final long serialVersionUID = -6986128600085710578L;

	private String compositeTransactionId;
	private String activityId;
	private BusinessActivityStatus status;

	public BusinessActivityMessage(){
		super();
	}
	
	public BusinessActivityMessage(String compositeTransactionId, String activityId, BusinessActivityStatus status) {
		super();
		this.compositeTransactionId = compositeTransactionId;
		this.activityId = activityId;
		this.status = status;
	}
	
	public String getCompositeTransactionId() {
		return compositeTransactionId;
	}

	public void setCompositeTransactionId(String compositeTransactionId) {
		this.compositeTransactionId = compositeTransactionId;
	}

	public String getActivityId() {
		return activityId;
	}

	public void setActivityId(String activityId) {
		this.activityId = activityId;
	}

	public BusinessActivityStatus getStatus() {
		return status;
	}

	public void setStatus(BusinessActivityStatus status) {
		this.status = status;
	}

}
