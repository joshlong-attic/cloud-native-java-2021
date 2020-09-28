package com.example.customerservice;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.Collection;

@RestController
@SpringBootApplication
public class CustomerServiceApplication {

    private final Collection<Customer> customers = new ArrayList<>();

    CustomerServiceApplication() {
        var names = "Kimly,Tammie,Mark,Luke,Bean,Kai,Zhen,Justin".split(",");
        for (var i = 0; i < 100; i++)
            this.customers.add(new Customer(i + 1, names[(int) (Math.random() * (names.length - 1))]));

    }

    @GetMapping("/customers")
    Flux<Customer> getCustomers() {
        return Flux.fromIterable(this.customers);
    }

    public static void main(String[] args) {
        SpringApplication.run(CustomerServiceApplication.class, args);
    }

}

@Data
@AllArgsConstructor
@NoArgsConstructor
class Customer {

    private Integer id;
    private String name;

}
