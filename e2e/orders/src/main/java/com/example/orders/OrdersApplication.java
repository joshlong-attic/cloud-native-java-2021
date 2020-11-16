package com.example.orders;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@RestController
@SpringBootApplication
public class OrdersApplication {

	public static void main(String[] args) {
		SpringApplication.run(OrdersApplication.class, args);
	}

	private final Map<Integer, Collection<Order>> orders = new ConcurrentHashMap<>();

	@Bean
	Consumer<Map<String, Integer>> updatesConsumer() {
		return message -> {
			var cid = message.get("customer-deletion");
			this.orders.remove(cid);
			System.out.println("removed orders for customer # " + cid);
		};
	}

	@GetMapping("/orders/{customerId}")
	Flux<Order> getOrdersForCustomer(@PathVariable Integer customerId) {
		var results = this.orders.getOrDefault(customerId, new ArrayList<>());
		return Flux.fromIterable(results);
	}

	@PostConstruct
	void begin() {

		for (var customerId = 1; customerId <= 8; customerId++) {
			var orders = new ArrayList<Order>();
			for (var orderId = 1; orderId <= (Math.random() * 100); orderId++) {
				orders.add(new Order(orderId, customerId));
			}
			this.orders.put(customerId, orders);
		}

	}

}

@Data
@AllArgsConstructor
@NoArgsConstructor
class Order {
	private Integer id, customerId;
}