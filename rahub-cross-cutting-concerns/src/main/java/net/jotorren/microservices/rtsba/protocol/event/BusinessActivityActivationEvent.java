package net.jotorren.microservices.rtsba.protocol.event;

import net.jotorren.microservices.rtsba.CoordinationContextParticipant;

public class BusinessActivityActivationEvent extends BusinessActivityEvent {
	private static final long serialVersionUID = 7427120471883486039L;

	private CoordinationContextParticipant participant;
	
	public BusinessActivityActivationEvent(
			String transactionIdentifier, 
			String activityId, 
			CoordinationContextParticipant participant,
			long txSeqNumber) {
		super(transactionIdentifier, activityId);
		this.participant = participant;
	}

	public CoordinationContextParticipant getParticipant() {
		return participant;
	}
}
