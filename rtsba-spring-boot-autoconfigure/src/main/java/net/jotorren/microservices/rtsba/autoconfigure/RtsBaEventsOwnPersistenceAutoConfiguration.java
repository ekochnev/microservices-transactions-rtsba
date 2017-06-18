package net.jotorren.microservices.rtsba.autoconfigure;

import javax.sql.DataSource;

import org.axonframework.common.jdbc.ConnectionProvider;
import org.axonframework.spring.jdbc.SpringDataSourceConnectionProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import net.jotorren.microservices.rtsba.participant.RtsBaTransactional;

@Configuration
@ConditionalOnClass(RtsBaTransactional.class)
@ConditionalOnProperty("rtsba.datasource.url")
public class RtsBaEventsOwnPersistenceAutoConfiguration {
	
	@Bean
	@Primary
	@ConfigurationProperties(prefix="spring.datasource")
	public DataSource dataSource() {
	    return DataSourceBuilder.create().build();
	}

	@Bean
	@ConfigurationProperties(prefix="rtsba.datasource")
	public DataSource rtsbaDataSource() {
	    return DataSourceBuilder.create().build();
	}
	
    @Bean
    @ConditionalOnMissingBean
    public ConnectionProvider eventConnectionProvider() {
        return new SpringDataSourceConnectionProvider(rtsbaDataSource());
    }
}
