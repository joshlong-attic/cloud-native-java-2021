package com.example.configuration.vault;

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
		System.setProperty("VAULT_TOKEN", "00000000-0000-0000-0000-000000000000");
		SpringApplication.run(ConfigurationApplication.class, args);
	}

	@Bean
	ApplicationRunner applicationRunner(@Value("${message-from-vault-server:}") String valueFromVaultServer) {
		return args -> {
			log.info("message from the Spring Cloud Vault Server : " + valueFromVaultServer);
		};
	}
}