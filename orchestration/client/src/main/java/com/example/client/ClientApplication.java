package com.example.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerExchangeFilterFunction;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Collection;

@SpringBootApplication
@RestController
public class ClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClientApplication.class, args);
    }

    @Bean
    WebClient webClient(
            WebClient.Builder builder,
            ReactorLoadBalancerExchangeFilterFunction lbFunction) {
        return builder.filter(lbFunction).build();
    }
}

@RestController
@RequiredArgsConstructor
class CustomerOrdersRestController {

    private final CrmClient client;

    @GetMapping("/customer-orders")
    Flux<CustomerOrders> get() {
        Flux<Customer> customers = client.getCustomers();
        return customers
                .flatMap(c -> Flux.zip(Mono.just(c), client.getOrdersFor(c.getId()).collectList()))
                .map(tuple -> new CustomerOrders(tuple.getT1(), tuple.getT2()))
              /*  .retryWhen(Retry.backoff(10, Duration.ofSeconds(1)))
                .onErrorResume( ex ->Flux.empty())
                .timeout(Duration.ofSeconds(10))*/;
    }


}

@Data
@RequiredArgsConstructor
class CustomerOrders {
    private final Customer customer;
    private final Collection<Order> orders;
}

@Component
@RequiredArgsConstructor
class CrmClient {

    private final WebClient http;

    Flux<Customer> getCustomers() {
        return http
                .get()
                .uri("http://customer-service/customers")
                .retrieve()
                .bodyToFlux(Customer.class);
    }

    Flux<Order> getOrdersFor(Integer customerId) {
        return http.get().uri("http://order-service/orders/{customerId}", customerId).retrieve().bodyToFlux(Order.class);
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

@Data
@AllArgsConstructor
@NoArgsConstructor
class Customer {

    private Integer id;
    private String name;

}
