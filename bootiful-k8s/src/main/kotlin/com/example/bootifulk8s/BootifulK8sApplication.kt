package com.example.bootifulk8s

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.availability.AvailabilityChangeEvent
import org.springframework.boot.availability.ReadinessState
import org.springframework.boot.runApplication
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Bean
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.body
import org.springframework.web.reactive.function.server.router
import reactor.core.publisher.Mono
import java.time.Duration

@SpringBootApplication
class BootifulK8sApplication {

  @Bean
  fun http(publisher: ApplicationEventPublisher) = router {




    GET("/slow") {
      val slowReply = Mono
          .just("Hello world!")
          .delayElement(Duration.ofSeconds(10))
      ServerResponse.ok().body(slowReply)
    }

    // readiness probes
    GET("/down") {
      AvailabilityChangeEvent.publish(
          publisher,
          this,
          ReadinessState.REFUSING_TRAFFIC
      )
      ServerResponse.ok().bodyValue("down")
    }

    GET("/up") {
      AvailabilityChangeEvent.publish(
          publisher,
          this,
          ReadinessState.ACCEPTING_TRAFFIC
      )
      ServerResponse.ok().bodyValue("up")
    }

    // basics
    GET("/hello") {
      ServerResponse.ok().bodyValue(mapOf("message" to "Hello, world!"))
    }


  }
}

fun main(args: Array<String>) {
  runApplication<BootifulK8sApplication>(*args)
}
