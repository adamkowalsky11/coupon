package com.dev.coupon;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class OpenApiIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void exposesOpenApiDocument() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.info.title").value("Coupon API"))
				.andExpect(jsonPath("$.paths['/api/v1/coupons']").exists())
				.andExpect(jsonPath("$.paths['/api/v1/coupons'].get").exists())
				.andExpect(jsonPath("$.paths['/api/v1/coupons/{code}']").exists())
				.andExpect(jsonPath("$.paths['/api/v1/coupons/{code}/couponusage']").exists());
	}

	@Test
	void exposesSwaggerUi() throws Exception {
		mockMvc.perform(get("/swagger-ui/index.html"))
				.andExpect(status().isOk());
	}

	@Test
	void redirectsRootToSwaggerUi() throws Exception {
		mockMvc.perform(get("/"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/swagger-ui/index.html"));
	}

	@Test
	void describesCreateCouponRequestExamplesAndConstraints() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.components.schemas.CreateCouponRequest.properties.code.example")
						.value("SUMMER-2026"))
				.andExpect(jsonPath("$.components.schemas.CreateCouponRequest.properties.maxUsages.example")
						.value(100))
				.andExpect(jsonPath("$.components.schemas.CreateCouponRequest.properties.maxUsages.minimum")
						.value(1))
				.andExpect(jsonPath("$.components.schemas.CreateCouponRequest.properties.country.example")
						.value("PL"))
				.andExpect(jsonPath("$.components.schemas.CreateCouponRequest.properties.country.pattern")
						.value("[A-Za-z]{2}"));
	}

	@Test
	void describesForwardedForHeaderForCouponUsage() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("\"name\":\"X-Forwarded-For\"")))
				.andExpect(content().string(containsString("\"example\":\"83.16.0.1\"")));
	}

	@Test
	void describesUseCouponRequestExample() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.components.schemas.UseCouponRequest.properties.userId.example")
						.value("user-123"))
				.andExpect(jsonPath("$.components.schemas.CouponResponse.properties.code.example")
						.value("SUMMER-2026"));
	}
}
