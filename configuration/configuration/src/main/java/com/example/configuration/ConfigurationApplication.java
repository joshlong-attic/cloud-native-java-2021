package com.example.configuration;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;

@Log4j2
@SpringBootApplication
@EnableConfigurationProperties(BootifulProperties.class)
public class ConfigurationApplication {

	public static void main(String[] args) {
//		SpringApplication.run(ConfigurationApplication.class, args);
		new SpringApplicationBuilder()
			.sources(ConfigurationApplication.class)
//			.initializers(applicationContext -> applicationContext.getEnvironment().getPropertySources().addLast(new BootifulPropertySource()))
			.run(args);

	}

	@Autowired
	void contributeToPropertySources(ConfigurableEnvironment environment) {
		environment.getPropertySources().addLast(new BootifulPropertySource());
	}

	@Bean
	ApplicationRunner applicationRunner(
		Environment environment,
		@Value("${HOME}") String userHome,
		@Value("${spring.datasource.url}") String springDataSourceUrl,
		@Value("${message-from-program-args:}") String messageFromProgramArgs,
		@Value("${greetings-message:Default Hello: ${message-from-application-properties} }") String defaultValue,
		@Value("${bootiful-message}") String bootifulMessage,
		@Value("${message-from-config-server:}") String valueFromConfigServer,
		@Value("${message-from-vault-server:}") String valueFromVaultServer,
		BootifulProperties bootifulProperties
	) {
		return args -> {

			log.info("message from application.properties " + environment.getProperty("message-from-application-properties"));
			log.info("default value from application.properties " + defaultValue);
			log.info("user home from the environment variables: " + userHome);
			log.info("message from program args : " + messageFromProgramArgs);
			log.info("spring.datasource.url: " + springDataSourceUrl);
			log.info("message from custom PropertySource: " + bootifulMessage);
			log.info("message from @ConfigurationProperties " + bootifulProperties.getMessage());
			log.info("message from the Spring Cloud Config Server : " + valueFromConfigServer);
			log.info("message from the Spring Cloud Vault Server : " + valueFromVaultServer);
		};
	}

	static class BootifulPropertySource extends PropertySource<String> {

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
}


@Data
@RequiredArgsConstructor
@ConstructorBinding
@ConfigurationProperties("bootiful")
class BootifulProperties {
	private final String message;
}
