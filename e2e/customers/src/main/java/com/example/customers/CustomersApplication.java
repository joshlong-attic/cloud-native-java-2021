package com.example.customers;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.LivenessState;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.data.annotation.Id;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

@SpringBootApplication
public class CustomersApplication {

	public static void main(String[] args) {
		SpringApplication.run(CustomersApplication.class, args);
	}

	@Bean
	RouterFunction<ServerResponse> routes(ApplicationContext ac, CustomerRepository customerRepository) {
		return route()
			.GET("/down", serverRequest -> {
				AvailabilityChangeEvent.publish(ac, this, LivenessState.BROKEN);
				return ServerResponse.ok().bodyValue("down");
			})
			.GET("/customers", r -> ok().body(customerRepository.findAll(), Customer.class))
			.build();
	}

	@Bean
	Consumer<Flux<Integer>> customerDeletionsConsumer(CustomerRepository customerRepository) {
		return customerIds ->
			customerIds
				.doOnNext(cid -> System.out.println("deleting orders for customerId # " + cid))
				.flatMap(customerRepository::deleteById)
				.subscribe();
	}

	@Bean
	ApplicationListener<ApplicationReadyEvent> ready(CustomerRepository cr) {
		return event -> {
	/*		var id = new AtomicInteger();
			var delete = cr.deleteAll();
			var names = Flux
				.just("Yuxin", "Spencer", "Madhura", "Olga", "Violetta", "Stéphane", "Rob", "Josh")
				.map(name -> new Customer(null, name))
				.flatMap(cr::save);
			var all = cr.findAll();
			delete
				.thenMany(names)
				.thenMany(all)
				.onErrorResume(ex -> Flux.error(new RuntimeException("")))
				.subscribe(System.out::println);*/
		};
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