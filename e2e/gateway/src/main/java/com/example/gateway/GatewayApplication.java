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
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.annotation.Id;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

@SpringBootApplication
public class GatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(GatewayApplication.class, args);
	}

	@Bean
	RouteLocator gw(RouteLocatorBuilder rlb) {
		return rlb
			.routes()
			.route(rs -> rs //
				.path("/proxy")
				.filters(fs -> fs.setPath("/customers").addResponseHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*"))
				.uri("lb://customers/")
			)
			.build();
	}

	@Bean
	WebClient webClient(WebClient.Builder builder, LoadBalancedExchangeFilterFunction ef) {
		return builder.filter(ef).build();
	}

	private final Sinks.Many<Map<String, Object>> customerUpdates = Sinks.many().multicast().onBackpressureBuffer();

	@Bean
	Supplier<Flux<Map<String, Object>>> customerUpdatesSupplier() {
		return customerUpdates::asFlux;
	}

	private Mono<ServerResponse> handle(ServerRequest serverRequest) {
		var customerId = Integer.parseInt(serverRequest.pathVariable("customerId"));
		var emitResult = customerUpdates.tryEmitNext(Collections.singletonMap("customer-deletion", customerId));
		return ServerResponse.ok().bodyValue(
			Collections.singletonMap("success", emitResult.isSuccess()) );
	}

	@Bean
	RouterFunction<ServerResponse> routes(CrmClient crm) {
		return route()
			.GET("/cos", serverRequest -> ok().body(crm.getCustomerOrders(), CustomerOrders.class))
			.DELETE("/cos/{customerId}", this::handle)
			.build();
	}

}

// aggregate view
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

	private final WebClient webClient;

	Flux<Customer> getCustomers() {
		return this.webClient.get().uri("http://customers/customers").retrieve().bodyToFlux(Customer.class);
	}

	Flux<Order> getOrderForCustomer(Integer customerId) {
		return this.webClient.get().uri("http://orders/orders/{cid}", customerId).retrieve().bodyToFlux(Order.class);
	}

	Flux<CustomerOrders> getCustomerOrders() {
		return this.getCustomers()
			.flatMap(c -> Flux.zip(Mono.just(c), getOrderForCustomer(c.getId()).collectList()))
			.map(tpl -> new CustomerOrders(tpl.getT1(), tpl.getT2()));
	}

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
class Customer {

	@Id
	private Integer id;
	private String name;
}
