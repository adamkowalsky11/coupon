package com.dev.coupon.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

	@Bean
	OpenAPI couponOpenApi() {
		return new OpenAPI()
				.info(new Info()
						.title("Coupon API")
						.version("v1")
						.description("Coupon creation, lookup and usage API"));
	}
}
