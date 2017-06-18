package net.jotorren.microservices.rtsba.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.SerializationFeature;

import net.jotorren.microservices.rtsba.RtsBaProperties;
import net.jotorren.microservices.rtsba.participant.RtsBaDataHolder;
import net.jotorren.microservices.rtsba.participant.RtsBaTransactional;
import net.jotorren.microservices.rtsba.participant.aop.RtsBaAspect;
import net.jotorren.microservices.rtsba.participant.error.RtsBaControllerExceptionHandler;
import net.jotorren.microservices.rtsba.participant.error.RtsBaResponseErrorHandler;
import net.jotorren.microservices.rtsba.protocol.BusinessActivityProtocol;
import net.jotorren.microservices.rtsba.protocol.CoordinationContextProtocol;

@Configuration
@ConditionalOnClass(RtsBaTransactional.class)
@EnableConfigurationProperties(RtsBaProperties.class)
public class RtsBaAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public RtsBaControllerExceptionHandler rtsBaControllerExceptionHandler() {
		return new RtsBaControllerExceptionHandler();
	}
	
	@Bean
	@ConditionalOnMissingBean
	public RtsBaResponseErrorHandler rtsBaResponseErrorHandler() {
		return new RtsBaResponseErrorHandler();
	}
	
	@Bean
	@ConditionalOnMissingBean
	public RtsBaDataHolder rtsbaDataHolder() {
		return new RtsBaDataHolder();
	}

	@Bean
	@ConditionalOnMissingBean
	public CoordinationContextProtocol rtsbaCoordContextProtocol() {
		return new CoordinationContextProtocol();
	}
	
	@Bean
	@ConditionalOnMissingBean
	public BusinessActivityProtocol rtsbaActivityProtocol() {
		return new BusinessActivityProtocol();
	}	
	
	@Bean
	@ConditionalOnMissingBean
	public RtsBaAspect rtsbaAspect() {
		return new RtsBaAspect();
	}
	
	@Bean
	@ConditionalOnMissingBean(name = "rtsbaTemplate")
	public RestTemplate rtsbaTemplate(RestTemplateBuilder builder, RtsBaResponseErrorHandler rtsBaResponseErrorHandler) {
		MappingJackson2HttpMessageConverter jsonHttpMessageConverter = new MappingJackson2HttpMessageConverter();
		jsonHttpMessageConverter.getObjectMapper().configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
		return builder
				.additionalMessageConverters(jsonHttpMessageConverter)
				.errorHandler(rtsBaResponseErrorHandler)
				.build();
	}
}
