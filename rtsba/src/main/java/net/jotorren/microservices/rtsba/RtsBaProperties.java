package net.jotorren.microservices.rtsba;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rtsba")
public class RtsBaProperties {

//	@Value("${rtsba.endpoint}")
	private String endpoint			= "http://localhost:8090/rtsba";
	
//	@Value("${rtsba.activation-uri}")
	private String activationUri	= "/activation";
	
//	@Value("${rtsba.registration-uri}")
	private String registrationUri	= "/registration";
	
//	@Value("${rtsba.status-uri}")
	private String statusUri		= "/closed";
	
//	@Value("${rtsba.transaction-timeout}")
	private long transactionTimeout	= 60000;

//	@Value("${rtsba.open-timeout}")
	private long openTimeout	= 5000;
	
//	@Value("${rtsba.activation-timeout}")
	private long activationTimeout	= 5000;

	public String getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}

	public String getActivationUri() {
		return activationUri;
	}

	public void setActivationUri(String activationUri) {
		this.activationUri = activationUri;
	}

	public String getRegistrationUri() {
		return registrationUri;
	}

	public void setRegistrationUri(String registrationUri) {
		this.registrationUri = registrationUri;
	}

	public String getStatusUri() {
		return statusUri;
	}

	public void setStatusUri(String statusUri) {
		this.statusUri = statusUri;
	}

	public long getTransactionTimeout() {
		return transactionTimeout;
	}

	public void setTransactionTimeout(long transactionTimeout) {
		this.transactionTimeout = transactionTimeout;
	}

	public long getOpenTimeout() {
		return openTimeout;
	}

	public void setOpenTimeout(long openTimeout) {
		this.openTimeout = openTimeout;
	}
	
	public long getActivationTimeout() {
		return activationTimeout;
	}

	public void setActivationTimeout(long activationTimeout) {
		this.activationTimeout = activationTimeout;
	}
}
