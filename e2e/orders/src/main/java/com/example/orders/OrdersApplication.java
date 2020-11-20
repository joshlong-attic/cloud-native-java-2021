package com.example.orders;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Controller
@SpringBootApplication
public class OrdersApplication {

	private final Map<Integer, Collection<Order>> db = new ConcurrentHashMap<>();

	@Bean
	Consumer<Flux<Integer>> customerDeletionsConsumer() {
		return customerIds ->
			customerIds
				.doOnNext(cid -> System.out.println("deleting orders for customerId # " + cid))
				.map(db::remove)
				.subscribe();
	}

	public static void main(String[] args) {
		SpringApplication.run(OrdersApplication.class, args);
	}

	@MessageMapping("orders/{customerId}")
	Flux<Order> getOrdersFor(@DestinationVariable Integer customerId) {
		return Flux.fromIterable(db.get(customerId));
	}

	@Bean
	RouterFunction<ServerResponse> routes() {
		return route()
			.GET("/orders/{customerId}", serverRequest -> {
				var customerId = Integer.parseInt(serverRequest.pathVariable("customerId"));
				var orders = Flux.fromIterable(db.get(customerId));
				return ServerResponse.ok().body(orders, Order.class);
			})
			.build();
	}

	@PostConstruct
	void setup() {
		for (var customerId = 149; customerId <= 156; customerId++) {
			var orders = new ArrayList<Order>();
			for (var orderId = 1; orderId <= (Math.random() * 100); orderId++) {
				orders.add(new Order(orderId, customerId));
			}
			this.db.put(customerId, orders);
		}
	}
}


@Data
@AllArgsConstructor
@NoArgsConstructor
class Order {

	private Integer id;
	private Integer customerId;
}