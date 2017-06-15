package net.jotorren.microservices.rtsba.participant.error;

import javax.transaction.TransactionalException;

public class RtsBaException extends TransactionalException {
	private static final long serialVersionUID = 3794180170270039313L;
	
	private String code;
	
	public RtsBaException(String c, String s, Throwable throwable) {
		super(s, throwable);
		this.code = c;
	}

	public String getCode() {
		return code;
	}
}
