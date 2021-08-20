package com.example.edge;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.graphql.data.method.annotation.*;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

@SpringBootApplication
public class EdgeApplication {

	public static void main(String[] args) {
		SpringApplication.run(EdgeApplication.class, args);
	}

	@Bean
	RSocketRequester rSocketRequester(RSocketRequester.Builder builder) {
		return builder.tcp("localhost", 8181);
	}

	@Bean
	WebClient webClient(WebClient.Builder builder) {
		return builder.build();
	}

	@Bean
	RouteLocator gateway(RouteLocatorBuilder rlb) {
		return
			rlb
				.routes()
				.route(rs -> rs
					.path("/proxy").and().host("*.spring.io")
					.filters(fs -> fs
						.setPath("/customers")
						.addResponseHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
						.retry(10)
					)
					.uri("http://localhost:8080/")
				)
				.build();
	}
}

@RestController
@RequiredArgsConstructor
class CrmRestController {

	private final CrmClient crmClient;

	@GetMapping("/cos")
	Flux<CustomerOrders> get() {
		return this.crmClient.getCustomerOrders();
	}
}

@RequiredArgsConstructor
@Component
class CrmClient {

	private final WebClient http;

	private final RSocketRequester rSocket;

	Flux<Order> getOrdersFor(Integer customerId) {
		return this.rSocket
			.route("orders.{customerId}", customerId)
			.retrieveFlux(Order.class);
	}

	Flux<Customer> getCustomers() {
		return this.http.get().uri("http://localhost:8080/customers").retrieve().bodyToFlux(Customer.class);
	}

	Flux<CustomerOrders> getCustomerOrders() {
		return this.getCustomers()
			.flatMap(customer -> Mono.zip(
					Mono.just(customer),
					getOrdersFor(customer.getId()).collectList()
				)
			)
			.map(tuple2 -> new CustomerOrders(tuple2.getT1(), tuple2.getT2()));
	}

}

@Data
@AllArgsConstructor
@NoArgsConstructor
class CustomerOrders {

	private Customer customer;
	private List<Order> orders;
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class Order {
	private Integer id, customerId;
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class Customer {


	private Integer id;
	private String name;
}


@GraphQlController
@RequiredArgsConstructor
class CrmGraphqlController {

	private final CrmClient crmClient;

	// queries
	// subscriptions
	// mutations

	//	@SchemaMapping (typeName = "Query"  ,field = "customers")
	@QueryMapping
	Flux<Customer> customers() {
		return this.crmClient.getCustomers();
	}

	@SchemaMapping(typeName = "Customer")
	Mono<Boolean> onPto(Customer customer) {
		return Mono.just(false)
			.delayElement(Duration.ofSeconds(3));
	}

	@MutationMapping
	Mono<Customer> addCustomer(@Argument String name) {
		System.out.println("adding " + name + '.');
		return Mono.just(new Customer(1, name));
	}

	@SchemaMapping(typeName = "Customer")
	Flux<Order> orders(Customer customer) {
		return this.crmClient
			.getOrdersFor(customer.getId());
	}


}