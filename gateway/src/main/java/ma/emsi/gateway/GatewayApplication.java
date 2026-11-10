package ma.emsi.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

@SpringBootApplication
public class GatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

    @Bean
    @Profile("!static")
    RouteLocator clientRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("clients-eureka", r -> r.path("/clients", "/clients/**")
                        .uri("lb://SERVICE-CLIENT"))
                .route("client-eureka", r -> r.path("/client/**")
                        .uri("lb://SERVICE-CLIENT"))
                .build();
    }
}
