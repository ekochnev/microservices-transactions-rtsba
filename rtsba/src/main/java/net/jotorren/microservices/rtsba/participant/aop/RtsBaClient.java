package net.jotorren.microservices.rtsba.participant.aop;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import net.jotorren.microservices.context.ThreadLocalContext;
import net.jotorren.microservices.rtsba.protocol.CoordinationContextMessageHeader;

public class RtsBaClient {
	
	public static final String RTSBA_CLIENT 		= "rtsba.client";
	public static final String RTSBA_TRANSACTIONAL 	= "rtsba.transactional";
	public static final String RTSBA_CONTEXT_URI 	= "rtsba.context.uri";
	public static final String RTSBA_CONTEXT_ID 	= "rtsba.context.id";
	public static final String RTSBA_REGISTRATION 	= "rtsba.registration";
	
	private long sequence = 0;
	
	public HttpHeaders generateHeaders() {
		HttpHeaders headers = new HttpHeaders();
		
		if (ThreadLocalContext.get(RTSBA_TRANSACTIONAL, Boolean.class).booleanValue()){
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.set(CoordinationContextMessageHeader.RTSBA_CONTEXT, 
					"<" + ThreadLocalContext.get(RTSBA_CONTEXT_URI, String.class) + ">; " +
							CoordinationContextMessageHeader.RTSBA_CONTEXT_RELATION);
			
			headers.set(CoordinationContextMessageHeader.RTSBA_REGISTER, 
					ThreadLocalContext.get(RTSBA_REGISTRATION, String.class));
			headers.set(CoordinationContextMessageHeader.RTSBA_SEQUENCE, Long.toString(this.sequence));
		}
		
		return headers;
	}
	
	public RtsBaClient(long initialSequence) {
		this.sequence = initialSequence;
	}
	
	public <T> HttpEntity<T> encode(T entity) {
		HttpEntity<T> httpEntity = new HttpEntity<T>(entity, generateHeaders());
		this.sequence+=4; // potential messages for a new participant: register, completion, suspend, resume
		return httpEntity;
	}
	
	public long protocol() {
		long callSequence = this.sequence;
		this.sequence++;
		return callSequence;
	}
}
