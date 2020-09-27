package com.example.configuration.refresh;


import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Log4j2
@SpringBootApplication
public class ConfigurationApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigurationApplication.class, args);
    }
}


@Log4j2
@Component
@RefreshScope
class Refreshable {

    Refreshable(@Value("${message-from-config-server}") String msg) {
        log.info("value(s) in " + ConfigurationProperties.class.getName() + " bean is: " + msg);
    }
}

@Log4j2
@Component
class RefreshListener {

    @EventListener
    public void refreshed(RefreshScopeRefreshedEvent rsre) {
        log.info("something has changed! " + rsre.getName());
    }
}
