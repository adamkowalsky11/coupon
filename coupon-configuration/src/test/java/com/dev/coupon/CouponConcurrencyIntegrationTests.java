package com.dev.coupon;

import com.dev.coupon.application.command.CreateCouponCommand;
import com.dev.coupon.application.command.UseCouponCommand;
import com.dev.coupon.application.exception.CouponAlreadyUsedException;
import com.dev.coupon.application.port.in.CreateCouponUseCase;
import com.dev.coupon.application.port.in.GetCouponUseCase;
import com.dev.coupon.application.port.in.UseCouponUseCase;
import com.dev.coupon.application.port.out.GeoLocationPort;
import com.dev.coupon.domain.CountryCode;
import com.dev.coupon.domain.CouponExpiredException;
import com.dev.coupon.application.view.CouponView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class CouponConcurrencyIntegrationTests {

	private static final String CLIENT_IP = "203.0.113.10";
	private static final int TIMEOUT_SECONDS = 20;

	@Autowired
	private CreateCouponUseCase createCouponUseCase;

	@Autowired
	private GetCouponUseCase getCouponUseCase;

	@Autowired
	private UseCouponUseCase useCouponUseCase;

	@MockitoBean
	private GeoLocationPort geoLocationPort;

	@BeforeEach
	void stubGeoLocation() {
		when(geoLocationPort.resolveCountry(anyString())).thenReturn(CountryCode.of("PL"));
	}

	@Test
	void neverAllowsMoreConcurrentUsagesThanLimit() throws Exception {
		int limit = 5;
		int requests = 10;
		String code = uniqueCode("LIMIT");
		createCouponUseCase.create(new CreateCouponCommand(code, limit, "PL"));

		List<Outcome> outcomes = runConcurrently(
				code,
				requests,
				index -> "user-" + index
		);

		assertThat(outcomes).filteredOn(Outcome.SUCCESS::equals).hasSize(limit);
		assertThat(outcomes).filteredOn(Outcome.EXPIRED::equals).hasSize(requests - limit);

		CouponView coupon = getCouponUseCase.get(code);
		assertThat(coupon.currentUsages()).isEqualTo(limit);
		assertThat(coupon.remainingUsages()).isZero();
	}

	@Test
	void allowsOnlyOneConcurrentUsagePerUser() throws Exception {
		int limit = 5;
		int requests = 8;
		String code = uniqueCode("USER");
		createCouponUseCase.create(new CreateCouponCommand(code, limit, "PL"));

		List<Outcome> outcomes = runConcurrently(
				code,
				requests,
				index -> "same-user"
		);

		assertThat(outcomes).filteredOn(Outcome.SUCCESS::equals).hasSize(1);
		assertThat(outcomes).filteredOn(Outcome.ALREADY_USED::equals).hasSize(requests - 1);

		CouponView coupon = getCouponUseCase.get(code);
		assertThat(coupon.currentUsages()).isEqualTo(1);
		assertThat(coupon.remainingUsages()).isEqualTo(limit - 1);
	}

	private List<Outcome> runConcurrently(
			String code,
			int requests,
			IntFunction<String> userIds
	) throws Exception {
		ExecutorService executor = Executors.newFixedThreadPool(requests);
		CountDownLatch ready = new CountDownLatch(requests);
		CountDownLatch start = new CountDownLatch(1);

		try {
			List<Future<Outcome>> futures = IntStream.range(0, requests)
					.mapToObj(index -> executor.submit(() -> {
						ready.countDown();
						if (!start.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
							throw new IllegalStateException("Concurrent start timed out");
						}
						try {
							useCouponUseCase.use(new UseCouponCommand(
									code,
									userIds.apply(index),
									CLIENT_IP
							));
							return Outcome.SUCCESS;
						} catch (CouponExpiredException exception) {
							return Outcome.EXPIRED;
						} catch (CouponAlreadyUsedException exception) {
							return Outcome.ALREADY_USED;
						}
					}))
					.toList();

			assertThat(ready.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
			start.countDown();

			return futures.stream()
					.map(this::await)
					.toList();
		} finally {
			executor.shutdownNow();
		}
	}

	private Outcome await(Future<Outcome> future) {
		try {
			return future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new AssertionError("Interrupted while waiting for usage result", exception);
		} catch (ExecutionException exception) {
			throw new AssertionError("Concurrent usage failed", exception.getCause());
		} catch (TimeoutException exception) {
			throw new AssertionError("Concurrent usage timed out", exception);
		}
	}

	private String uniqueCode(String prefix) {
		String suffix = UUID.randomUUID()
				.toString()
				.substring(0, 8)
				.toUpperCase(Locale.ROOT);
		return "CONC-" + prefix + "-" + suffix;
	}

	private enum Outcome {
		SUCCESS,
		EXPIRED,
		ALREADY_USED
	}
}
