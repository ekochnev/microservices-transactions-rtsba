package net.jotorren.microservices.rtsba.protocol.event;

import net.jotorren.microservices.rtsba.CoordinationContextParticipant;

public class CoordinationContextRegistrationEvent extends CoordinationContextEvent {
	private static final long serialVersionUID = 5820830397259822738L;

	private CoordinationContextParticipant participant;
	
	public CoordinationContextRegistrationEvent(String compositeTransactionId, CoordinationContextParticipant participant) {
		super(compositeTransactionId);
		this.participant = participant;
	}

	public CoordinationContextParticipant getParticipant() {
		return participant;
	}
	
	public String getActivityId() {
		return Integer.toHexString(this.participant.hashCode());
	}
}
