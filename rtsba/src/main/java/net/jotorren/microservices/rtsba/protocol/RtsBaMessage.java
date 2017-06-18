package net.jotorren.microservices.rtsba.protocol;

import net.jotorren.microservices.rtsba.protocol.BusinessActivityMessageType;

public enum RtsBaMessage {

	NONE 			(0),
	CLOSE 			(BusinessActivityMessageType.CLOSE),
	COMPENSATE 		(BusinessActivityMessageType.COMPENSATE),
	CANCEL 			(BusinessActivityMessageType.CANCEL),
	FAILED 			(BusinessActivityMessageType.FAILED),
	EXITED 			(BusinessActivityMessageType.EXITED),
	NOT_COMPLETED 	(BusinessActivityMessageType.NOT_COMPLETED),
	ALL 			(999);
	
	private long sequenceNumber;
	
	RtsBaMessage(long sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
	}

	public long getSequenceNumber() {
		return sequenceNumber;
	}
}
