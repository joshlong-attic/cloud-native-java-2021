package com.example.customers;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

@SpringBootApplication
public class CustomersApplication {

	public static void main(String[] args) {
		SpringApplication.run(CustomersApplication.class, args);
	}


	@Bean
	Consumer<Flux<Integer>> customerDeletionsConsumer(CustomerRepository customerRepository) {
		return customerIds ->
			customerIds
				.doOnNext(cid -> System.out.println("customerId to delete: " + cid))
				.flatMap(customerRepository::deleteById)
				.subscribe();
	}
	
	@Bean
	RouterFunction<ServerResponse> routes(ApplicationContext context, CustomerRepository cr) {
		return route()
			.GET("/customers", r -> ok().body(cr.findAll(), Customer.class))
			.GET("/down", r -> {
				AvailabilityChangeEvent.publish(context, LivenessState.BROKEN);
				return ServerResponse.ok().bodyValue(Collections.singletonMap("down", true));
			})
			.build();
	}

	@Bean
	ApplicationListener<AvailabilityChangeEvent> availabilityChangeEventApplicationListener() {
		return availabilityChangeEvent -> System.out.println(availabilityChangeEvent.getState().toString());
	}

	@Bean
	ApplicationListener<ApplicationReadyEvent> ready(@Value("${hello}") String hello,
																																																		DatabaseClient dbc, CustomerRepository repository) {
		return event -> {
			System.out.println("Hello, " + hello + "!");
			var names = Flux
				.just("Madhura", "Olga", "Yuxin", "Violetta", "Dr. Syer", "Stéphane", "Spencer", "Josh")
				.map(name -> new Customer(null, name))
				.flatMap(repository::save);
			var sql = """
				 create table CUSTOMER(
				 	id serial primary key, 
				 	name varchar(255) not null
					);
				""";
			dbc.sql(sql).fetch().rowsUpdated()
				.thenMany(names)
				.thenMany(repository.findAll())
				.subscribe(System.out::println);
		};
	}

}

/*
@RestController
@RequiredArgsConstructor
class CustomerRestController {

	private final CustomerRepository customerRepository;

	@GetMapping("/customers")
	Flux<Customer> getCustomers() {
		return this.customerRepository.findAll();
	}
}*/


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
