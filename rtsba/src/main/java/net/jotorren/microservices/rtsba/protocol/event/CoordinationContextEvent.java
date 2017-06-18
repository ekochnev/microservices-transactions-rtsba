package net.jotorren.microservices.rtsba.protocol.event;

import java.io.Serializable;

public abstract class CoordinationContextEvent implements Serializable {
	private static final long serialVersionUID = -2669420125302845403L;

	private String compositeTransactionId;
	
	public CoordinationContextEvent(String compositeTransactionId){
		this.compositeTransactionId = compositeTransactionId;
	}

	public String getCompositeTransactionId() {
		return compositeTransactionId;
	}
}
