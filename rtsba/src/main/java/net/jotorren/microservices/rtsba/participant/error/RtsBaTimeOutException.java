package net.jotorren.microservices.rtsba.participant.error;

import javax.transaction.TransactionalException;

public class RtsBaTimeOutException extends TransactionalException {
	private static final long serialVersionUID = 3682419024657247285L;
	
	private String code;
	
	public RtsBaTimeOutException(String c, String s, Throwable throwable) {
		super(s, throwable);
		this.code = c;
	}

	public String getCode() {
		return code;
	}
}
