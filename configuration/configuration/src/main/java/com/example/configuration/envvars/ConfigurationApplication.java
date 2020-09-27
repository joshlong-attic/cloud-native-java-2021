package com.example.configuration.envvars;

import lombok.extern.log4j.Log4j2;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

@Log4j2
@SpringBootApplication
public class ConfigurationApplication {

	public static void main(String[] args) {
		// simulate program arguments
		String[] actualArgs = new String[]{"spring.datasource.url=jdbc:postgres://localhost/some-prod-db"};
		SpringApplication.run(ConfigurationApplication.class, actualArgs);
	}

	@Bean
	ApplicationRunner applicationRunner(Environment environment) {
		return args -> {
			log.info("our database URL connection will be " + environment.getProperty("spring.datasource.url"));
		};
	}
}