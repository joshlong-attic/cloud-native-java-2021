package com.example.edge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;

@SpringBootApplication
public class EdgeApplication {

	@Bean
	RouteLocator gateway(RouteLocatorBuilder rlb) {
		return rlb
			.routes()
			.route(rs ->
				rs
					.path("/proxy").and().host("*.spring.io") // <- predicate
					.filters(fs -> fs
						.setPath("/customers")
						.retry(10)
						.addResponseHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*") // CORS
					)
					.uri("http://localhost:8080/")
			)
			.build();
	}

	public static void main(String[] args) {
		SpringApplication.run(EdgeApplication.class, args);
	}

	@Bean
	WebClient webClient(WebClient.Builder builder) {
		return builder.build();
	}

	@Bean
	RSocketRequester rSocketRequester(RSocketRequester.Builder builder) {
		return builder.tcp("localhost", 8181);
	}

}


@Controller
class CrmGraphQlController {

	private final CrmClient crm;

	CrmGraphQlController(CrmClient crm) {
		this.crm = crm;
	}

	@SchemaMapping(typeName = "Customer", field = "orders")
	Flux<Order> orders(Customer customer) {
		return crm.getOrdersFor(customer.id());
	}

//	@SchemaMapping(typeName = "Query", field = "customers")
	@QueryMapping
	Flux<Customer> customers() {
		return this.crm.getCustomers();
	}
}


@Controller
@ResponseBody
class CrmRestController {

	private final CrmClient crm;

	CrmRestController(CrmClient crm) {
		this.crm = crm;
	}

	@GetMapping("/cos")
	Flux<CustomerOrder> getCustomerOrders() {
		return this.crm.getCustomerOrders();
	}
}

@Component
class CrmClient {

	private final RSocketRequester rSocket;
	private final WebClient http;

	CrmClient(RSocketRequester rSocket, WebClient http) {
		this.rSocket = rSocket;
		this.http = http;
	}

	Flux<CustomerOrder> getCustomerOrders() {
		return getCustomers()
			.flatMap(c -> Mono.zip(
				Mono.just(c),
				getOrdersFor(c.id()).collectList()
			))
			.map(tuple2 -> new CustomerOrder(tuple2.getT1(), tuple2.getT2()));
	}

	Flux<Order> getOrdersFor(Integer customerId) {
		return this.rSocket.route("orders.{customerId}", customerId).retrieveFlux(Order.class)
			.retryWhen(Retry.backoff(10, Duration.ofSeconds(1)))
			.onErrorResume(ex -> Flux.empty())
			.timeout(Duration.ofSeconds(1))
			;
	}

	Flux<Customer> getCustomers() {
		return this.http.get().uri("http://localhost:8080/customers").retrieve().bodyToFlux(Customer.class)
			.retryWhen(Retry.backoff(10, Duration.ofSeconds(1)))
			.onErrorResume(ex -> Flux.empty())
			.timeout(Duration.ofSeconds(1))
			;
	}

}


record Customer(Integer id, String name) {
}

record Order(Integer id, Integer customerId) {
}

record CustomerOrder(Customer customer, List<Order> orders) {
}



