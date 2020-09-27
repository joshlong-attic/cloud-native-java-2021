package com.example.configuration.propertysource;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;

@Log4j2
@SpringBootApplication
public class ConfigurationApplication {

	public static void main(String[] args) {
/*		new SpringApplicationBuilder()
			.sources(ConfigurationApplication.class)
			.initializers(context -> context
				.getEnvironment()
				.getPropertySources()
				.addLast(new BootifulPropertySource())
			)
			.run(args);*/
		SpringApplication.run(ConfigurationApplication.class, args);
	}

	@Bean
	ApplicationRunner applicationRunner(@Value("${bootiful-message}") String bootifulMessage) {
		return args -> {
			log.info("message from custom PropertySource: " + bootifulMessage);
		};
	}

	@Autowired
	void contributeToTheEnvironment(ConfigurableEnvironment environment) {
		environment.getPropertySources().addLast(new BootifulPropertySource());
	}
}

class BootifulPropertySource extends PropertySource<String> {

	BootifulPropertySource() {
		super("bootiful");
	}

	@Override
	public Object getProperty(String name) {

		if (name.equalsIgnoreCase("bootiful-message")) {
			return "Hello from " + BootifulPropertySource.class.getSimpleName() + "!";
		}

		return null;
	}
}