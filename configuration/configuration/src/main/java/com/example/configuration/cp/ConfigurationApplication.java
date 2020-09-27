package com.example.configuration.cp;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Log4j2
@SpringBootApplication
@EnableConfigurationProperties(BootifulProperties.class)
public class ConfigurationApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigurationApplication.class, args);
    }

    @Bean
    ApplicationRunner applicationRunner(BootifulProperties bootifulProperties) {
        return args -> {
            log.info("message from @ConfigurationProperties bootifulcp.message: " + bootifulProperties.getMessage());
            log.info("message from @ConfigurationProperties bootifulcp.favorite-number: " + bootifulProperties.getFavoriteNumber());
        };
    }

}

@RefreshScope
@Data
@RequiredArgsConstructor
@ConfigurationProperties("bootifulcp")
class BootifulProperties {
    private int favoriteNumber;
    private String message;
}
