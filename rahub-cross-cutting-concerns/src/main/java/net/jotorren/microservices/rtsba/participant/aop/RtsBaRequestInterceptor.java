package net.jotorren.microservices.rtsba.participant.aop;

import java.io.IOException;
import java.util.Map;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import net.jotorren.microservices.context.ThreadLocalContext;

public class RtsBaRequestInterceptor implements ClientHttpRequestInterceptor{

	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
			throws IOException {
		RtsBaClient rtsBaClient = ThreadLocalContext.get(RtsBaClient.RTSBA_CLIENT, RtsBaClient.class);
		
		if (null != rtsBaClient) {
			Map<String, String> rtsbaHeaders = rtsBaClient.generateHeaders().toSingleValueMap();
			for (String header : rtsbaHeaders.keySet()) {
				request.getHeaders().set(header, rtsbaHeaders.get(header));
			}
			rtsBaClient.protocol(); // register
			rtsBaClient.protocol(); // completion
			rtsBaClient.protocol(); // potential suspend
			rtsBaClient.protocol(); // potential resume
		}
		
		return execution.execute(request, body);
	}

}
