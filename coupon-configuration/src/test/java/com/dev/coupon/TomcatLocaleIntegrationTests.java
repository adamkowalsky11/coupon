package com.dev.coupon;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TomcatLocaleIntegrationTests {

	@Value("${local.server.port}")
	private int port;

	@Test
	void acceptsMoreThanTenLocalesInAcceptLanguageHeader() throws Exception {
		String acceptLanguage = "pl-PL,pl;q=0.9,en-US;q=0.8,en;q=0.7,de;q=0.6,fr;q=0.5,"
				+ "es;q=0.4,it;q=0.3,nl;q=0.2,cs;q=0.1,sv;q=0.1";
		HttpRequest request = HttpRequest.newBuilder(
						URI.create("http://localhost:" + port + "/swagger-ui/index.html"))
				.header("Accept-Language", acceptLanguage)
				.GET()
				.build();

		try (HttpClient client = HttpClient.newHttpClient()) {
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

			assertThat(response.statusCode()).isEqualTo(200);
			assertThat(response.body()).contains("Swagger UI");
		}
	}
}
