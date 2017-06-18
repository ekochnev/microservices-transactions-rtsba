package net.jotorren.microservices.rtsba.participant.error;

import java.io.IOException;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.jotorren.microservices.rs.ErrorDetails;
import net.jotorren.microservices.rs.JavaSerializer;

public class RtsBaResponseErrorHandler extends DefaultResponseErrorHandler {

	private static final Logger LOG = LoggerFactory.getLogger(RtsBaResponseErrorHandler.class);

	@SuppressWarnings("resource")
	@Override
	public void handleError(ClientHttpResponse clienthttpresponse) throws IOException {
		if (clienthttpresponse.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR) {
			Scanner s = new Scanner(clienthttpresponse.getBody()).useDelimiter("\\A");
			String body = s.hasNext() ? s.next() : null;
			s.close();
			if (null == body){
				LOG.error("Uncontrolled error {}", clienthttpresponse.getRawStatusCode());
				super.handleError(clienthttpresponse);
			} else {
				ErrorDetails error = null;
				try {
					ObjectMapper mapper = new ObjectMapper();
					error = mapper.readValue(body, ErrorDetails.class);
				} catch(Exception e){
					LOG.error("Unable to deserialize received error", e);
					super.handleError(clienthttpresponse);
					return;
				}
				
				Throwable th = null;
				if (null != error && null != error.getReason()){
					try {
						th = (Throwable) JavaSerializer.fromString(error.getReason());
					} catch (IOException | ClassNotFoundException e) {
						LOG.error("Unable to deserialize reason of transactional exception", e);
						super.handleError(clienthttpresponse);
						return;
					}
				}
				throw new RtsBaException(error.getCode(), error.getMessage(), th);
			}
		} else {
			super.handleError(clienthttpresponse);
		}
	}
}
