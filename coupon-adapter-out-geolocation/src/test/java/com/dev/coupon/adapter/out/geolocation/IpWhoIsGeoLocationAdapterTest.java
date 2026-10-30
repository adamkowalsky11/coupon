package com.dev.coupon.adapter.out.geolocation;

import com.dev.coupon.application.exception.GeoLocationUnavailableException;
import com.dev.coupon.domain.CountryCode;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IpWhoIsGeoLocationAdapterTest {

	private static final String IP_ADDRESS = "8.8.8.8";

	private WireMockServer wireMock;
	private MutableClock clock;
	private IpWhoIsGeoLocationAdapter adapter;

	@BeforeEach
	void setUp() {
		wireMock = new WireMockServer(options().dynamicPort());
		wireMock.start();
		clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
		adapter = adapter(Duration.ofMillis(500), 2);
	}

	@AfterEach
	void tearDown() {
		adapter.close();
		if (wireMock.isRunning()) {
			wireMock.stop();
		}
	}

	@Test
	void resolvesCountryCodeFromIpWhoIs() {
		stubSuccessfulLookup();

		assertThat(adapter.resolveCountry(IP_ADDRESS)).isEqualTo(CountryCode.of("PL"));
		wireMock.verify(1, getRequestedFor(urlEqualTo("/" + IP_ADDRESS)));
	}

	@Test
	void usesCachedCountryWithoutCallingServiceAgain() {
		stubSuccessfulLookup();

		adapter.resolveCountry(IP_ADDRESS);
		adapter.resolveCountry(IP_ADDRESS);

		wireMock.verify(1, getRequestedFor(urlEqualTo("/" + IP_ADDRESS)));
	}

	@Test
	void retriesAfterServerError() {
		wireMock.stubFor(get(urlEqualTo("/" + IP_ADDRESS))
				.inScenario("retry")
				.whenScenarioStateIs(Scenario.STARTED)
				.willReturn(aResponse().withStatus(500))
				.willSetStateTo("success"));
		wireMock.stubFor(get(urlEqualTo("/" + IP_ADDRESS))
				.inScenario("retry")
				.whenScenarioStateIs("success")
				.willReturn(successfulResponse()));

		assertThat(adapter.resolveCountry(IP_ADDRESS)).isEqualTo(CountryCode.of("PL"));
		wireMock.verify(2, getRequestedFor(urlEqualTo("/" + IP_ADDRESS)));
	}

	@Test
	void returnsCachedCountryWhenServiceFailsAfterRefreshWindow() {
		stubSuccessfulLookup();
		adapter.resolveCountry(IP_ADDRESS);

		clock.advance(Duration.ofHours(2));
		wireMock.stop();

		assertThat(adapter.resolveCountry(IP_ADDRESS)).isEqualTo(CountryCode.of("PL"));
	}

	@Test
	void failsWhenServiceIsUnavailableAndThereIsNoCachedValue() {
		wireMock.stop();

		assertThatThrownBy(() -> adapter.resolveCountry(IP_ADDRESS))
				.isInstanceOf(GeoLocationUnavailableException.class);
	}

	@Test
	void failsWhenIpWhoIsReportsUnsuccessfulLookupWithoutRetrying() {
		wireMock.stubFor(get(urlEqualTo("/" + IP_ADDRESS))
				.willReturn(okJson("""
						{"success":false,"message":"Reserved range"}
						""")));

		assertThatThrownBy(() -> adapter.resolveCountry(IP_ADDRESS))
				.isInstanceOf(GeoLocationUnavailableException.class);
		wireMock.verify(1, getRequestedFor(urlEqualTo("/" + IP_ADDRESS)));
	}

	@Test
	void failsWhenServiceExceedsTimeLimit() {
		adapter.close();
		adapter = adapter(Duration.ofMillis(100), 1);
		wireMock.stubFor(get(urlEqualTo("/" + IP_ADDRESS))
				.willReturn(successfulResponse().withFixedDelay(1000)));

		assertThatThrownBy(() -> adapter.resolveCountry(IP_ADDRESS))
				.isInstanceOf(GeoLocationUnavailableException.class);
	}

	private IpWhoIsGeoLocationAdapter adapter(Duration timeLimit, int retryMaxAttempts) {
		return new IpWhoIsGeoLocationAdapter(new GeoLocationSettings(
				wireMock.baseUrl(),
				Duration.ofMillis(500),
				Duration.ofMillis(500),
				Duration.ofHours(1),
				Duration.ofHours(24),
				timeLimit,
				retryMaxAttempts,
				Duration.ofMillis(10)
		), clock);
	}

	private void stubSuccessfulLookup() {
		wireMock.stubFor(get(urlEqualTo("/" + IP_ADDRESS))
				.willReturn(successfulResponse()));
	}

	private com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder successfulResponse() {
		return okJson("""
				{"success":true,"country_code":"PL"}
				""");
	}

	private static final class MutableClock extends Clock {

		private Instant instant;

		private MutableClock(Instant instant) {
			this.instant = instant;
		}

		private void advance(Duration duration) {
			instant = instant.plus(duration);
		}

		@Override
		public ZoneId getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(ZoneId zone) {
			return this;
		}

		@Override
		public Instant instant() {
			return instant;
		}
	}
}
