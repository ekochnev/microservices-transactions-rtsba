package net.jotorren.microservices.rtsba.autoconfigure;

import javax.sql.DataSource;

import org.axonframework.common.jdbc.ConnectionProvider;
import org.axonframework.spring.jdbc.SpringDataSourceConnectionProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import net.jotorren.microservices.rtsba.participant.RtsBaTransactional;

@Configuration
@ConditionalOnClass(RtsBaTransactional.class)
@ConditionalOnMissingBean(name = "rtsbaDataSource")
public class RtsBaEventsPersistenceAutoConfiguration {

	@Autowired
	private DataSource dataSource;
	
    @Bean
    @ConditionalOnMissingBean
    public ConnectionProvider eventConnectionProvider() {
        return new SpringDataSourceConnectionProvider(dataSource);
    }
}
