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
import java.util.List;
import java.util.function.Supplier;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@SpringBootApplication
public class GatewayApplication {

	@Bean
	WebClient webClient(
		LoadBalancedExchangeFilterFunction eff,
		WebClient.Builder builder) {
		return builder.filter(eff).build();
	}


	private final Sinks.Many<Integer> deletions = Sinks.many().multicast().onBackpressureBuffer();

	@Bean
	Supplier<Flux<Integer>> customerDeletionsSupplier() {
		return deletions::asFlux;
	}

	@Bean
	RSocketRequester rSocketRequester(RSocketRequester.Builder builder) {
		return builder.tcp("localhost", 8181);
	}

	@Bean
	RouterFunction<ServerResponse> routes(CrmClient crmClient) {
		return route()
			.DELETE("/cos/{customerId}", serverRequest -> {
				var cid = Integer.parseInt(serverRequest.pathVariable("customerId"));
				return ServerResponse.ok().bodyValue(deletions.tryEmitNext(cid).isSuccess());
			})
			.GET("/cos", serverRequest -> {
				var customerOrders = crmClient.getCustomerOrders();
				return ServerResponse.ok().body(customerOrders, CustomerOrders.class);
			})
			.build();
	}

	@Bean
	RouteLocator myGateway(RouteLocatorBuilder rlb) {
		return rlb
			.routes()
			.route(routeSpec ->
				routeSpec
					.path("/proxy")
					.filters(filterSpec -> filterSpec
						.setPath("/customers")
						.retry(19)
						.addResponseHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
					)
					.uri("lb://customers/")
			)
			.build();
	}


	public static void main(String[] args) {
		SpringApplication.run(GatewayApplication.class, args);
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
		return getCustomers()
			.flatMap(customer -> Flux.zip(Mono.just(customer), getOrdersFor(customer.getId()).collectList()))
			.map(tuple -> new CustomerOrders(tuple.getT1(), tuple.getT2()));
	}


	Flux<Customer> getCustomers() {

//		Flux<String> host1 = null;
//		Flux<String> host2 = null;
//		Flux<String> host3 = null; /// todo
//		Flux<String> stringFlux = Flux.firstWithSignal(host1, host2, host3);


		return this.http.get().uri("http://customers/customers").retrieve().bodyToFlux(Customer.class)
			.timeout(Duration.ofSeconds(20))
			.retryWhen(Retry.backoff(10, Duration.ofSeconds(10)))
			.onErrorResume(exc -> Flux.empty());
	}

	Flux<Order> getOrdersFor(Integer customerId) {
		return this.rSocketRequester.route("orders.{customerId}", customerId).retrieveFlux(Order.class);
	}
}