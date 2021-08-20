package com.example.customers;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.annotation.Id;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@SpringBootApplication
public class CustomersApplication {

	public static void main(String[] args) {
		SpringApplication.run(CustomersApplication.class, args);
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

@Component
@RequiredArgsConstructor
class CustomerDataInitializer implements ApplicationRunner {

	private final CustomerRepository customerRepository;

	@Override
	public void run(ApplicationArguments args) throws Exception {

		var names = Flux.just("Rodney", "Praiksha", "Steve", "Sumin",
			"Vanessa", "Punith", "Madhuri", "Madhura");
		var customers = names.map(name -> new Customer(null, name));
		var saved = customers.flatMap(this.customerRepository::save);
		saved.subscribe(System.out::println);

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