package net.jotorren.microservices.rtsba;

import java.io.Serializable;

public class CoordinationContext implements Serializable {
	private static final long serialVersionUID = 9208126861676172533L;
	
	private String identifier;
	private String coordinationType;
	private RegistrationEndpoint registration;

	public CoordinationContext(){
		super();
	}
	
	public CoordinationContext(String identifier, String coordinationType, RegistrationEndpoint registration) {
		super();
		this.identifier = identifier;
		this.coordinationType = coordinationType;
		this.registration = registration;
	}

	public String getIdentifier() {
		return identifier;
	}

	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	public String getCoordinationType() {
		return coordinationType;
	}

	public void setCoordinationType(String coordinationType) {
		this.coordinationType = coordinationType;
	}

	public RegistrationEndpoint getRegistration() {
		return registration;
	}

	public void setRegistration(RegistrationEndpoint registration) {
		this.registration = registration;
	}

}
