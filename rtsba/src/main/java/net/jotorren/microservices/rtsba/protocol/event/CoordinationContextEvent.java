package net.jotorren.microservices.rtsba.protocol.event;

import java.io.Serializable;

public abstract class CoordinationContextEvent implements Serializable {
	private static final long serialVersionUID = -2669420125302845403L;

	private String coordinationContextId;
	
	public CoordinationContextEvent(String coordinationContextId){
		this.coordinationContextId = coordinationContextId;
	}

	public String getCoordinationContextId() {
		return this.coordinationContextId;
	}
}
