package com.example.serviceb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

@SpringBootApplication
public class ServiceBApplication {

	@Bean
	ApplicationListener <ApplicationReadyEvent> client (
			WebClient http ){
		return event -> {
			http
					.get()
					.uri("http://localhost:8080/message/Jane")
					.retrieve()
					.bodyToFlux(String.class)
					.subscribe(System.out::println);
		};
	}

    @Bean
    WebClient webClient(WebClient.Builder builder) {
        return builder.build();
    }

    public static void main(String[] args) {
        SpringApplication.run(ServiceBApplication.class, args);
    }

}
