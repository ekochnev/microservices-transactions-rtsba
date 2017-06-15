package net.jotorren.microservices.rtsba;

import java.io.Serializable;

public class CreateCoordinationContext implements Serializable {
	private static final long serialVersionUID = 3243244665032315394L;

	private long expires;
	private String currentContext;
	private Object additional;

	public long getExpires() {
		return expires;
	}

	public void setExpires(long expires) {
		this.expires = expires;
	}

	public String getCurrentContext() {
		return currentContext;
	}

	public void setCurrentContext(String currentContext) {
		this.currentContext = currentContext;
	}

	public Object getAdditional() {
		return additional;
	}

	public void setAdditional(Object additional) {
		this.additional = additional;
	}
}
