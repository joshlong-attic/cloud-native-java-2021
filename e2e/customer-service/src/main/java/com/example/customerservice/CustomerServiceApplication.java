package com.example.customerservice;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.data.annotation.Id;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.function.Consumer;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

@SpringBootApplication
public class CustomerServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(CustomerServiceApplication.class, args);
	}


	@Bean
	Consumer<Flux<Map<String, String>>> updatesConsumer(CustomerRepository customerRepository) {
		return streamOfUpdates -> streamOfUpdates
			.map(m -> m.get("customer-deletion"))
			.map(Integer::parseInt)
			.doOnNext(cid -> System.out.println("about to delete customer # " + cid))
			.flatMap(customerRepository::deleteById)
			.subscribe();
	}

	@Bean
	RouterFunction<ServerResponse> routes(CustomerRepository customerRepository) {
		return route()
			.GET("/customers", r -> ok().body(customerRepository.findAll(), Customer.class))
			.build();
	}

	@Bean
	ApplicationListener<ApplicationReadyEvent> ready(
		DatabaseClient dc,
		CustomerRepository cr) {
		return applicationReadyEvent -> {
			var sql = """
					create table if not exists customer( id serial primary key, name varchar(255) not null)
				""";
			var names = Flux
				.just("A", "B", "C", "D", "E")
				.map(name -> new Customer(null, name))
				.flatMap(cr::save);
			dc
				.sql(sql)
				.fetch()
				.rowsUpdated()
				.thenMany(names)
				.thenMany(cr.findAll())
				.subscribe(System.out::println);
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

