package net.jotorren.microservices.rtsba;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import net.jotorren.microservices.rtsba.protocol.RtsBaMessage;

public class CoordinationContextParticipant implements Serializable {
	private static final long serialVersionUID = 4289601866075700732L;
	
	private String address;
	private List<RtsBaMessage> protocolEvents;
	
	// the hashcode depends on the timestamp which is immutable
	// two participants with the same address will have different hashes
	// if they are instantiated in different moments (milliseconds precision)
	private long timeStamp;
	
	public CoordinationContextParticipant() {
		this.timeStamp = System.currentTimeMillis();
	}
	
	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public List<RtsBaMessage> getProtocolEvents() {
		return protocolEvents;
	}

	public void setProtocolEvents(List<RtsBaMessage> protocolEvents) {
		this.protocolEvents = protocolEvents;
	}

	@Override
	public int hashCode() {
		return Objects.hash(address, timeStamp, protocolEvents);
	}
}
