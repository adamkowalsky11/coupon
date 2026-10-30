package com.dev.coupon.adapter.out.geolocation;

import com.dev.coupon.application.exception.GeoLocationUnavailableException;
import com.dev.coupon.application.port.out.GeoLocationPort;
import com.dev.coupon.domain.CountryCode;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

public final class IpWhoIsGeoLocationAdapter implements GeoLocationPort, AutoCloseable {

	private static final String SERVICE_NAME = "ipwhois";
	private static final String UNAVAILABLE_MESSAGE = "Geo location service is unavailable";
	private static final float FAILURE_RATE_THRESHOLD = 50.0f;
	private static final int SLIDING_WINDOW_SIZE = 10;
	private static final int MINIMUM_NUMBER_OF_CALLS = 5;
	private static final Duration WAIT_DURATION_IN_OPEN_STATE = Duration.ofSeconds(30);

	private final RestClient restClient;
	private final Cache<String, CachedCountry> cache;
	private final CircuitBreaker circuitBreaker;
	private final Retry retry;
	private final TimeLimiter timeLimiter;
	private final ExecutorService executor;
	private final Clock clock;
	private final Duration cacheRefreshAfter;

	public IpWhoIsGeoLocationAdapter(GeoLocationSettings settings, Clock clock) {
		Objects.requireNonNull(settings, "settings");
		this.clock = Objects.requireNonNull(clock, "clock");
		this.cacheRefreshAfter = settings.cacheRefreshAfter();

		HttpClient httpClient = HttpClient.newBuilder()
				.connectTimeout(settings.connectTimeout())
				.build();
		JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
		requestFactory.setReadTimeout(settings.readTimeout());
		this.restClient = RestClient.builder()
				.baseUrl(normalizeBaseUrl(settings.baseUrl()))
				.requestFactory(requestFactory)
				.build();

		this.cache = Caffeine.newBuilder()
				.expireAfterWrite(settings.cacheExpireAfter())
				.build();
		this.circuitBreaker = CircuitBreaker.of(SERVICE_NAME, CircuitBreakerConfig.custom()
				.failureRateThreshold(FAILURE_RATE_THRESHOLD)
				.slidingWindowSize(SLIDING_WINDOW_SIZE)
				.minimumNumberOfCalls(MINIMUM_NUMBER_OF_CALLS)
				.waitDurationInOpenState(WAIT_DURATION_IN_OPEN_STATE)
				.build());
		this.retry = Retry.of(SERVICE_NAME, RetryConfig.custom()
				.maxAttempts(settings.retryMaxAttempts())
				.waitDuration(settings.retryWaitDuration())
				.ignoreExceptions(GeoLocationUnavailableException.class, CallNotPermittedException.class)
				.build());
		this.timeLimiter = TimeLimiter.of(SERVICE_NAME, TimeLimiterConfig.custom()
				.timeoutDuration(settings.timeLimit())
				.cancelRunningFuture(true)
				.build());
		this.executor = Executors.newVirtualThreadPerTaskExecutor();
	}

	@Override
	public CountryCode resolveCountry(String ipAddress) {
		CachedCountry cached = cache.getIfPresent(ipAddress);
		if (cached != null && !needsRefresh(cached)) {
			return cached.country();
		}

		try {
			CountryCode country = fetchWithResilience(ipAddress);
			cache.put(ipAddress, new CachedCountry(country, clock.instant()));
			return country;
		} catch (GeoLocationUnavailableException exception) {
			if (cached != null) {
				return cached.country();
			}
			throw exception;
		} catch (RuntimeException exception) {
			if (cached != null) {
				return cached.country();
			}
			throw new GeoLocationUnavailableException(UNAVAILABLE_MESSAGE, exception);
		}
	}

	@Override
	public void close() {
		executor.shutdownNow();
	}

	private CountryCode fetchWithResilience(String ipAddress) {
		Supplier<CountryCode> supplier = () -> fetchCountry(ipAddress);
		Callable<CountryCode> timeLimited = TimeLimiter.decorateFutureSupplier(
				timeLimiter,
				() -> CompletableFuture.supplyAsync(supplier, executor)
		);
		Callable<CountryCode> circuitBroken = CircuitBreaker.decorateCallable(circuitBreaker, timeLimited);
		Callable<CountryCode> retried = Retry.decorateCallable(retry, circuitBroken);

		try {
			return retried.call();
		} catch (GeoLocationUnavailableException exception) {
			throw exception;
		} catch (Exception exception) {
			throw new GeoLocationUnavailableException(UNAVAILABLE_MESSAGE, exception);
		}
	}

	private CountryCode fetchCountry(String ipAddress) {
		IpWhoIsResponse response = restClient.get()
				.uri("/{ipAddress}", ipAddress)
				.accept(MediaType.APPLICATION_JSON)
				.retrieve()
				.body(IpWhoIsResponse.class);

		if (response == null || !response.success()) {
			throw new GeoLocationUnavailableException(UNAVAILABLE_MESSAGE);
		}

		try {
			return CountryCode.of(response.countryCode());
		} catch (IllegalArgumentException exception) {
			throw new GeoLocationUnavailableException(UNAVAILABLE_MESSAGE, exception);
		}
	}

	private boolean needsRefresh(CachedCountry cached) {
		return !cached.fetchedAt().plus(cacheRefreshAfter).isAfter(clock.instant());
	}

	private String normalizeBaseUrl(String baseUrl) {
		return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
	}

	private record CachedCountry(CountryCode country, Instant fetchedAt) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record IpWhoIsResponse(
			boolean success,
			@JsonProperty("country_code") String countryCode
	) {
	}
}
