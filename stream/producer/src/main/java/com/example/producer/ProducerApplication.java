package com.example.producer;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.messaging.Source;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@EnableBinding(Source.class)
@SpringBootApplication
@RequiredArgsConstructor
public class ProducerApplication {

    private final Source source;

    @GetMapping("/send/{name}")
    Mono<Boolean> send(@PathVariable String name) {
        var msg = MessageBuilder
                .withPayload("Hello,  " + name + "!")
                .build();
        return Mono.just(this.source.output().send(msg));
    }

    public static void main(String[] args) {
        SpringApplication.run(ProducerApplication.class, args);
    }

}
