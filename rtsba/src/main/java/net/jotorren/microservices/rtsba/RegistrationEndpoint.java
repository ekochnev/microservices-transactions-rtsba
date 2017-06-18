package net.jotorren.microservices.rtsba;

import java.io.Serializable;

import org.springframework.http.HttpMethod;

public class RegistrationEndpoint implements Serializable {
	private static final long serialVersionUID = -5799814952238311505L;

	private String address;
	private String contentType;
	private HttpMethod method;

	public RegistrationEndpoint() {
		super();
	}

	public RegistrationEndpoint(String address, String contentType, HttpMethod method) {
		super();
		this.address = address;
		this.contentType = contentType;
		this.method = method;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public HttpMethod getMethod() {
		return method;
	}

	public void setMethod(HttpMethod method) {
		this.method = method;
	}

}
