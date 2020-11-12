package wag;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.LivenessState;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.data.annotation.Id;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@SpringBootApplication(proxyBeanMethods = false)
public class WagApplication {

	public static void main(String[] args) {
		SpringApplication.run(WagApplication.class, args);
	}

	@Bean
	RouterFunction<ServerResponse> routes(ApplicationEventPublisher publisher) {
		return route()
			.GET("/up", r -> {
				AvailabilityChangeEvent.publish(publisher, this, LivenessState.CORRECT);
				return ServerResponse.ok().bodyValue("up");
			})
			.GET("/down", r -> {
				AvailabilityChangeEvent.publish(publisher, this, LivenessState.BROKEN);
				return ServerResponse.ok().bodyValue("down");
			})
			.build();
	}
	
	@Bean
	ApplicationListener<ApplicationReadyEvent> ready(DatabaseClient dbc, ReservationRepository rr) {
		return event -> rr
			.saveAll(Flux.just(new Reservation(null, "Andy"), new Reservation(null, "Josh")))
			.thenMany(rr.findAll())
			.subscribe(System.out::println);
	}

}

interface ReservationRepository extends ReactiveCrudRepository<Reservation, Integer> {
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class Reservation {

	@Id
	private Integer id;
	private String name;

}

@RestController
@RequiredArgsConstructor
class ReservationController {

	private final ReservationRepository reservationRepository;

	@GetMapping("/reservations")
	Flux<Reservation> reservations() {
		return this.reservationRepository.findAll();
	}
}

@RestController
class SlowController {

	@GetMapping("/slow")
	Mono<String> greet() {
		return Mono.just("Hello, world!").delayElement(Duration.ofSeconds(20));
	}
}

@RestController
class GreetingsController {

	@GetMapping("/")
	public Map<String, String> greet() {
		return Collections.singletonMap("greetings", "Olá!");
	}
}

