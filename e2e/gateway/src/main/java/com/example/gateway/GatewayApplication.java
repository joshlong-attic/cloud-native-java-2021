package com.example.gateway;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.loadbalancer.reactive.LoadBalancedExchangeFilterFunction;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;


@SpringBootApplication
public class GatewayApplication {

	private final Sinks.Many<Integer> customerDeletions = Sinks.many().multicast().onBackpressureBuffer();

	@Bean
	Supplier<Flux<Integer>> customerDeletionsSupplier() {
		return customerDeletions::asFlux;
	}

	@Bean
	RouterFunction<ServerResponse> routes(CrmClient crmClient) {
		return route()
			.DELETE("/cos/{customerId}", serverRequest -> {
				var customerId = Integer.parseInt(serverRequest.pathVariable("customerId"));
				var emitResult = customerDeletions.tryEmitNext(customerId);
				return ServerResponse.ok().bodyValue(Collections.singletonMap(customerId, emitResult.isSuccess()));
			})
			.GET("/cos", serverRequest -> ok().body(crmClient.getCustomerOrders(), CustomerOrders.class))
			.build();
	}

	@Bean
	RouteLocator routeLocator(RouteLocatorBuilder rlb) {
		return rlb
			.routes()
			.route(rs -> rs
				.path("/proxy")
				.filters(fs -> fs
					.setPath("/customers")
					.addResponseHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
				)
				.uri("lb://customers")
			)
			.build();
	}

	public static void main(String[] args) {
		SpringApplication.run(GatewayApplication.class, args);
	}


	@Bean
	RSocketRequester rSocketRequester(RSocketRequester.Builder builder) {
		return builder.connectTcp("localhost", 7777).block();
	}

	@Bean
	WebClient httpClient(WebClient.Builder b,
																						LoadBalancedExchangeFilterFunction eff) {
		return b.filter(eff).build();
	}

}


@Data
@AllArgsConstructor
@NoArgsConstructor
class Customer {
	private Integer id;
	private String name;
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class Order {
	private Integer id;
	private Integer customerId;
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class CustomerOrders {
	private Customer customer;
	private List<Order> orders;
}

@Component
@RequiredArgsConstructor
class CrmClient {

	private final RSocketRequester rSocketRequester;
	private final WebClient http;

	Flux<CustomerOrders> getCustomerOrders() {
		var tuple2Flux = getAllCustomers()
			.flatMap(customer -> Flux.zip(Mono.just(customer), getRSocketOrdersFor(customer.getId()).collectList()));
		return tuple2Flux.map(tuple -> new CustomerOrders(tuple.getT1(), tuple.getT2()));
	}

	Flux<Order> getRSocketOrdersFor(Integer customerId) {
		return this.rSocketRequester
			.route("orders/{customerId}", customerId)
			.retrieveFlux(Order.class);
	}

	Flux<Order> getOrdersFor(Integer customerId) {
		return this.http
			.get()
			.uri("http://orders/orders/{customerId}", customerId)
			.retrieve()
			.bodyToFlux(Order.class)
			.retryWhen(Retry.backoff(10, Duration.ofSeconds(1)))
			.onErrorResume(ex -> Flux.empty())
			.timeout(Duration.ofSeconds(20));
	}

	Flux<Customer> getAllCustomers() {
		return this.http.get().uri("http://customers/customers").retrieve().bodyToFlux(Customer.class);
	}

}