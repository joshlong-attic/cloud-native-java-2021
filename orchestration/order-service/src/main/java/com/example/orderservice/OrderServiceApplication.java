package com.example.orderservice;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.UUID;

@RestController
@SpringBootApplication
public class OrderServiceApplication {


    @GetMapping("/orders/{customerId}")
    Flux<Order> getOrdersFor(@PathVariable Integer customerId) {
        var count = Math.random() * 10;
        var orders = new ArrayList<Order>();
        for (var i = 0; i < count; i++)
            orders.add(new Order(customerId, UUID.randomUUID().toString(),
                    (float) (Math.random() * 100.0f)));
        return Flux.fromIterable(orders);
    }

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }

}


@Data
@AllArgsConstructor
@NoArgsConstructor
class Order {
    private Integer customerId;
    private String id;
    private float price;
}