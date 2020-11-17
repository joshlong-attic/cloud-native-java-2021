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
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@SpringBootApplication
public class GatewayApplication {

	@Bean
	WebClient webClient(WebClient.Builder builder, LoadBalancedExchangeFilterFunction exchangeFilterFunction) {
		return builder.filter(exchangeFilterFunction).build();
	}


	@Bean
	RouteLocator mySimpleGateway(RouteLocatorBuilder rlb) {
		return rlb
			.routes()

			.route(rs -> rs
				.path("/proxy")
				.filters(fs -> fs
					.setPath("/customers")
					.addResponseHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
				)
				.uri("lb://customers/")
			)
			.build();
	}

	@Bean
	RouterFunction<ServerResponse> routes(CrmClient crmClient) {

/*
	Flux<String> resultsFromSomeService = null ;//todo
	Flux<String> stringFlux = resultsFromSomeService
			.timeout(Duration.ofSeconds(10))
			.retryWhen(Retry.backoff(10, Duration.ofSeconds(1)))
			.onErrorResume(exc -> Flux.empty());
	*/

	/*	ReactiveDiscoveryClient reactiveDiscoveryClient = null ;//
		Flux<ServiceInstance> order = reactiveDiscoveryClient.getInstances("order");
		Flux<String> map = order.take(3).map(si -> si.getHost() + ':' + si.getPort());
		map.collectList().map( listOfServices -> {

		})
			.subscribe( );*/


		// hedging
//		Flux<String> host1 = null;
//		Flux<String> host2 = null;
//		Flux<String> host3 = null;
//		Flux<String> stringFlux = Flux.firstWithValue(host1, host2, host3);

		return route()
			.DELETE("/cos/{cid}", serverRequest -> {
				var cid = Integer.parseInt(serverRequest.pathVariable("cid"));
				var emitResult = objectMany.tryEmitNext(cid);
				return ServerResponse.ok().bodyValue(Collections.singletonMap(cid, emitResult.isSuccess()));
			})
			.GET("/cos", req -> ServerResponse.ok().body(crmClient.getCustomerOrders(), CustomerOrders.class))
			.build();
	}

	private final Sinks.Many<Integer> objectMany = Sinks.many().multicast().onBackpressureBuffer();

	@Bean
	Supplier<Flux<Integer>> customerDeletionsSupplier() {
		return objectMany::asFlux;
	}


	public static void main(String[] args) {
		SpringApplication.run(GatewayApplication.class, args);
	}

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

	Flux<Order> getOrdersFor(Integer customerId) {
		return this.webClient.get().uri("http://orders/orders/{customerId}", customerId).retrieve().bodyToFlux(Order.class);
	}

	Flux<Customer> getCustomers() {
		return this.webClient.get().uri("http://customers/customers").retrieve().bodyToFlux(Customer.class);
	}

	Flux<CustomerOrders> getCustomerOrders() {
		var customers = this.getCustomers();
		var customerAndOrders = customers.flatMap(customer -> Flux.zip(Mono.just(customer), getOrdersFor(customer.getId()).collectList()));
		return customerAndOrders.map(tuple -> new CustomerOrders(tuple.getT1(), tuple.getT2()));
	}

}
