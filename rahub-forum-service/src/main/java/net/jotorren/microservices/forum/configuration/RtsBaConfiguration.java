package net.jotorren.microservices.forum.configuration;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.SerializationFeature;

import net.jotorren.microservices.rtsba.participant.RtsBaDataHolder;
import net.jotorren.microservices.rtsba.participant.aop.RtsBaAspect;
import net.jotorren.microservices.rtsba.participant.error.RtsBaControllerExceptionHandler;
import net.jotorren.microservices.rtsba.participant.error.RtsBaResponseErrorHandler;
import net.jotorren.microservices.rtsba.protocol.BusinessActivityProtocol;
import net.jotorren.microservices.rtsba.protocol.CoordinationContextProtocol;

@Configuration
public class RtsBaConfiguration {

	@Bean
	public RtsBaControllerExceptionHandler rtsBaControllerExceptionHandler() {
		return new RtsBaControllerExceptionHandler();
	}
	
	@Bean
	public RtsBaDataHolder rtsbaDataHolder() {
		return new RtsBaDataHolder();
	}

	@Bean
	public CoordinationContextProtocol rtsbaCoordContextProtocol() {
		return new CoordinationContextProtocol();
	}
	
	@Bean
	public BusinessActivityProtocol rtsbaActivityProtocol() {
		return new BusinessActivityProtocol();
	}	
	
	@Bean
	public RtsBaAspect rtsbaAspect() {
		return new RtsBaAspect();
	}
	
	@Bean
	public RestTemplate rtsbaTemplate(RestTemplateBuilder builder) {
		MappingJackson2HttpMessageConverter jsonHttpMessageConverter = new MappingJackson2HttpMessageConverter();
		jsonHttpMessageConverter.getObjectMapper().configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
		return builder
				.additionalMessageConverters(jsonHttpMessageConverter)
				.errorHandler(new RtsBaResponseErrorHandler())
				.build();
	}
}
