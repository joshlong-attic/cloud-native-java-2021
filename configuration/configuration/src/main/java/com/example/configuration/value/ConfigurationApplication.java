package com.example.configuration.value;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@Log4j2
@SpringBootApplication
public class ConfigurationApplication {

	public static void main(String[] args) {
		SpringApplication.run(ConfigurationApplication.class, args);
	}

	@Bean
	ApplicationRunner applicationRunner(
		@Value("${message-from-application-properties:OOPS!}") String valueDoesExist,
		@Value("${mesage-from-application-properties:OOPS!}") String valueDoesNotExist) {
		return args -> {
			log.info("message from application.properties " + valueDoesExist);
			log.info("missing message from application.properties " + valueDoesNotExist);
		};
	}
}