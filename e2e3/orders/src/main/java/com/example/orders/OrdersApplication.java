package com.example.orders;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ResponseBody;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SpringBootApplication
public class OrdersApplication {

	public static void main(String[] args) {
		SpringApplication.run(OrdersApplication.class, args);
	}

}

record Order(Integer id, Integer customerId) {
}

@Controller
@ResponseBody
class OrderRestController {

	private final Map<Integer, Collection<Order>> db = new ConcurrentHashMap<>();

	OrderRestController() {
		for (var customerId = 1; customerId <= 8; customerId++) {
			var listOfOrders = new ArrayList<Order>();
			var max = (int) (Math.random() * 100);
			for (var orderId = 1; orderId <= max; orderId++)
				listOfOrders.add(new Order(orderId, customerId));
			this.db.put(customerId, listOfOrders);
		}
	}

	@MessageMapping("orders.{customerId}")
	Flux<Order> get(@DestinationVariable Integer customerId) {
		var listofOrders = this.db.get(customerId);
		return Flux.fromIterable(listofOrders);
	}
}


