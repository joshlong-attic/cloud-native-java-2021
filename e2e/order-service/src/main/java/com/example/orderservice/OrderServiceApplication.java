package com.example.orderservice;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@SpringBootApplication
public class OrderServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(OrderServiceApplication.class, args);
	}

	private final Map<Integer, List<Order>> orders = new ConcurrentHashMap<>();


	@Bean
	Consumer<Map<String, String>> updatesConsumer() {
		return message -> {
			Assert.state(message.containsKey("customer-deletion"),
				() -> "the incoming message should have a customer-deletion key");
			var cid = Integer.parseInt(message.get("customer-deletion"));
			this.orders.remove(cid);
			System.out.println  ( "removed orders for customer # " + cid );
		};
	}

	@PostConstruct
	void begin() {
		for (var i = 1; i <= 5; i++) {
			var listOfOrders = new ArrayList<Order>();
			for (var x = 1; x <= Math.max (10,  (int ) (Math.random() * 100)); x++)
				listOfOrders.add(new Order(x, i));
			this.orders.put(i, listOfOrders);
		}
	}

	@Bean
	RouterFunction<ServerResponse> routes() {
		return route()
			.GET("/orders/{customerId}", req -> {
				var customerId = Integer.parseInt(req.pathVariable("customerId"));
				var ordersForCustomer =  orders.containsKey( customerId) ? orders.get(customerId) : new ArrayList<Order>();
				var orderFlux = Flux.fromIterable(ordersForCustomer);
				return ServerResponse.ok().body(orderFlux, Order.class);
			})
			.build();
	}

}


@Data
@AllArgsConstructor
@NoArgsConstructor
class Order {

	private Integer id;
	private Integer customerId;
}
