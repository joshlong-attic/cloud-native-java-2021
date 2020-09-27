package com.example.configuration.profiles;

import lombok.extern.log4j.Log4j2;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

@Log4j2
@SpringBootApplication
public class ConfigurationApplication {

	public static void main(String[] args) {
		// this works
		// export SPRING_PROFILES_ACTIVE=dev
		// System.setProperty("spring.profiles.active", "dev"); // this works
		new SpringApplicationBuilder()
			.profiles("dev") // so does this
			.sources(ConfigurationApplication.class)
			.run(args);
	}

	@Bean
	ApplicationRunner applicationRunner(Environment environment) {
		return args -> {
			log.info("message from application.properties " + environment.getProperty("message-from-application-properties"));
		};
	}
}