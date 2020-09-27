package com.example.configuationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

// run this then curl http://localhost:8888/greetings/default
//
@EnableConfigServer
@SpringBootApplication
public class ConfiguationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfiguationServiceApplication.class, args);
    }
}
