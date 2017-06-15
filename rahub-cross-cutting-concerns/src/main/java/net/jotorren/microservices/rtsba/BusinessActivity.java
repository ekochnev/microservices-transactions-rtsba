package net.jotorren.microservices.rtsba;

import java.io.Serializable;

public class BusinessActivity implements Serializable {
	private static final long serialVersionUID = -1566801756183275534L;
	
	private String identifier;

	// We need the default constructor to allow JSON de/serialization
	public BusinessActivity() {
		this(null);
	}
	
	public BusinessActivity(String identifier) {
		super();
		this.identifier = identifier;
	}
	
	public String getIdentifier() {
		return identifier;
	}

	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}
	
}
