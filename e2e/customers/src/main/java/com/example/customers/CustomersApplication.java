package com.example.customers;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationRunner;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;

@SpringBootApplication
public class CustomersApplication {

	@Bean
	Consumer<Flux<Integer>> customerDeletionsConsumer(CustomerRepository cr) {
		return deletions -> deletions.flatMap(cr::deleteById).subscribe();
	}

	public static void main(String[] args) {
		SpringApplication.run(CustomersApplication.class, args);
	}

	@Bean
	ApplicationRunner ready(
		DatabaseClient dbc,
		CustomerRepository repository) {
		return event -> {


			var sql = """
				    
					 create table if not exists CUSTOMER ( 
					 	 id serial primary key not null, 
					 	 name varchar(255) not null
					 ) 
				""";

			var integerMono = dbc.sql(sql).fetch().rowsUpdated();

			var names = Flux
				.just("Yuxin", "Olga", "Stéphane", "Spencer", "Violetta", "Madhura", "Josh", "Dr. Syer")
				.map(name -> new Customer(null, name))
				.flatMap(repository::save);

			var all = repository.findAll();

			integerMono
				.thenMany(names)
				.thenMany(all)
				.subscribe(System.out::println);


		};
	}

}


interface CustomerRepository extends ReactiveCrudRepository<Customer, Integer> {
}

// data class Customer(val id: Int, name:String)
// case class Customer(val id: Int,  name:String)
// record Customer(INteger id, String name) {}

@Data
@AllArgsConstructor
@NoArgsConstructor
class Customer {

	@Id
	private Integer id;

	private String name;
}

@RestController
@RequiredArgsConstructor
class ProbesRestController {

	private final ApplicationContext context;

	@PostMapping("/down")
	Mono<Boolean> down() {
		AvailabilityChangeEvent.publish(this.context, LivenessState.BROKEN);
		return Mono.just(true);
	}
}

@RestController
@RequiredArgsConstructor
class CustomerRestController {

	private final CustomerRepository customerRepository;

	@GetMapping("/customers")
	Flux<Customer> get() {
		return this.customerRepository.findAll();
	}

}