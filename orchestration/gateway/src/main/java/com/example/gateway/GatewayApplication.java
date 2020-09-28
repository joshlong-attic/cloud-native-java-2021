package com.example.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;

@SpringBootApplication
public class GatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(GatewayApplication.class, args);
	}

	@Bean
	RouteLocator gateway(RouteLocatorBuilder rlb) {
		return rlb //
				.routes()//
				.route(routeSpec -> routeSpec // <2>
					  .path("/c")
						.filters( fs -> fs
								.setPath("/customers")
								.addResponseHeader (HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN , "*"))
						.uri("lb://customer-service/") // <4>
				) //
				.build();
	}
}
