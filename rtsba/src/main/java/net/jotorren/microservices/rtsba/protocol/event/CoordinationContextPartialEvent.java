package net.jotorren.microservices.rtsba.protocol.event;

public class CoordinationContextPartialEvent extends CoordinationContextEvent {
	private static final long serialVersionUID = 8934804004039735730L;

	private String stepIdentifier;
	
	public CoordinationContextPartialEvent(String coordinationContextId, String stepIdentifier) {
		super(coordinationContextId);
		this.stepIdentifier = stepIdentifier;
	}

	public String getStepIdentifier() {
		return stepIdentifier;
	}
}
