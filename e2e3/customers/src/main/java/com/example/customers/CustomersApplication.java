package com.example.customers;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.LivenessState;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.data.annotation.Id;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@SpringBootApplication
public class CustomersApplication {

	@Bean
	HealthIndicator healthIndicator() {
		return () -> Health.status("I <3 Production!").build();
	}

	public static void main(String[] args) {
		SpringApplication.run(CustomersApplication.class, args);
	}

}

@Controller
@ResponseBody
@RequiredArgsConstructor
class HealthRestController {

	private final ApplicationContext context;

	@GetMapping("/down")
	Mono<Void> down() {
		AvailabilityChangeEvent.publish(this.context, LivenessState.BROKEN);
		return Mono.empty();
	}
}

@Controller
@ResponseBody
@RequiredArgsConstructor
class CustomerRestController {

	private final CustomerRepository repository;

	@GetMapping("/customers")
	Flux<Customer> get() {
		return this.repository.findAll();
	}
}

interface CustomerRepository extends ReactiveCrudRepository<Customer, Integer> {

}

@Data
@AllArgsConstructor
@NoArgsConstructor
class Customer {

	@Id
	private Integer id;
	private String name;
}