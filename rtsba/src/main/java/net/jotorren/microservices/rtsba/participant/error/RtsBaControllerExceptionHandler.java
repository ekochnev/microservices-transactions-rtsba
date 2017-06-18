package net.jotorren.microservices.rtsba.participant.error;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import net.jotorren.microservices.rs.ErrorDetails;
import net.jotorren.microservices.rs.JavaSerializer;

@ControllerAdvice
public class RtsBaControllerExceptionHandler {

	private static final Logger LOG = LoggerFactory.getLogger(RtsBaControllerExceptionHandler.class);
	
	@ExceptionHandler(RtsBaException.class)
	public ResponseEntity<ErrorDetails> toResponse(Exception exception) throws IOException {
		HttpHeaders headersResp = new HttpHeaders();
		headersResp.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
		
		String reason = null;
		try {
			reason = JavaSerializer.toString(exception.getCause());
		} catch (IOException e) {
			LOG.error("Unable to serialize reason of transactional exception", e);
		}
		
		return new ResponseEntity<ErrorDetails>(
				new ErrorDetails(
						((RtsBaException)exception).getCode(), 
						exception.getMessage(), 
						reason, 
						toStackString(exception.getCause())), 
				headersResp, 
				HttpStatus.INTERNAL_SERVER_ERROR);	
	}

	public String toStackString(Throwable exception) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		exception.printStackTrace(pw);
		String stack = sw.toString();
		pw.close();
		
		return stack;
	}
}