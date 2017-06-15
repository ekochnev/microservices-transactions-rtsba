package net.jotorren.microservices.rs;

import java.io.Serializable;

public class ErrorDetails implements Serializable{
	private static final long serialVersionUID = -2902135610476337878L;
	
	private String code;
	private String message;
	private String reason;
	private String stack;

	public ErrorDetails() {
	}

	public ErrorDetails(String code, String message, String reason, String stack) {
		this.code = code;
		this.message = message;
		this.reason = reason;
		this.stack = stack;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
	
	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public String getStack() {
		return stack;
	}

	public void setStack(String stack) {
		this.stack = stack;
	}
}
