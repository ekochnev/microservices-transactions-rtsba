package net.jotorren.microservices.composite.configuration;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.SerializationFeature;

import net.jotorren.microservices.rtsba.participant.aop.RtsBaRequestInterceptor;
import net.jotorren.microservices.rtsba.participant.error.RtsBaResponseErrorHandler;

@Configuration
public class WebServicesConfiguration {
    
	@Bean
	public RestTemplate restTemplate(RestTemplateBuilder builder) {
		MappingJackson2HttpMessageConverter jsonHttpMessageConverter = new MappingJackson2HttpMessageConverter();
		jsonHttpMessageConverter.getObjectMapper().configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
		return builder
				.additionalMessageConverters(jsonHttpMessageConverter)
				.additionalInterceptors(new RtsBaRequestInterceptor())
				.errorHandler(new RtsBaResponseErrorHandler())
				.build();
	}
}
