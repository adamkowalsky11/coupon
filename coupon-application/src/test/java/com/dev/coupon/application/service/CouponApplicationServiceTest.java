package com.dev.coupon.application.service;

import com.dev.coupon.application.command.CreateCouponCommand;
import com.dev.coupon.application.command.UseCouponCommand;
import com.dev.coupon.application.exception.ClientIpNotResolvedException;
import com.dev.coupon.application.exception.CouponAlreadyUsedException;
import com.dev.coupon.application.exception.CouponCodeAlreadyExistsException;
import com.dev.coupon.application.exception.CouponNotFoundException;
import com.dev.coupon.application.exception.GeoLocationUnavailableException;
import com.dev.coupon.application.port.out.CouponRepository;
import com.dev.coupon.application.port.out.CouponUsageRepository;
import com.dev.coupon.application.port.out.GeoLocationPort;
import com.dev.coupon.application.view.CouponUsageView;
import com.dev.coupon.application.view.CouponView;
import com.dev.coupon.domain.CountryCode;
import com.dev.coupon.domain.CountryNotAllowedException;
import com.dev.coupon.domain.Coupon;
import com.dev.coupon.domain.CouponCode;
import com.dev.coupon.domain.CouponExpiredException;
import com.dev.coupon.domain.CouponId;
import com.dev.coupon.domain.CouponUsage;
import com.dev.coupon.domain.UsageLimit;
import com.dev.coupon.domain.UserId;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionOperations;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CouponApplicationServiceTest {

	private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
	private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
	private static final String CLIENT_IP = "203.0.113.10";

	private final CouponRepository couponRepository = mock(CouponRepository.class);
	private final CouponUsageRepository couponUsageRepository = mock(CouponUsageRepository.class);
	private final GeoLocationPort geoLocationPort = mock(GeoLocationPort.class);

	@Test
	void createsCouponWithNormalizedData() {
		when(couponRepository.existsByCode(any())).thenReturn(false);
		when(couponRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		CouponView view = service().create(new CreateCouponCommand("spring-2026", 5, "pl"));

		assertThat(view.code()).isEqualTo("SPRING-2026");
		assertThat(view.country()).isEqualTo("PL");
		assertThat(view.maxUsages()).isEqualTo(5);
		assertThat(view.currentUsages()).isZero();
		assertThat(view.remainingUsages()).isEqualTo(5);
		assertThat(view.createdAt()).isEqualTo(NOW);
		verify(couponRepository).existsByCode(CouponCode.of("SPRING-2026"));
		verify(couponRepository).save(any(Coupon.class));
	}

	@Test
	void rejectsDuplicateCouponCode() {
		when(couponRepository.existsByCode(any())).thenReturn(true);

		assertThatThrownBy(() -> service().create(new CreateCouponCommand("DUP-1", 5, "PL")))
				.isInstanceOf(CouponCodeAlreadyExistsException.class);

		verify(couponRepository, never()).save(any());
	}

	@Test
	void getsCouponByNormalizedCode() {
		Coupon coupon = coupon("GET-1", 5, 0, "PL");
		when(couponRepository.findByCode(CouponCode.of("GET-1"))).thenReturn(Optional.of(coupon));

		CouponView view = service().get("get-1");

		assertThat(view.id()).isEqualTo(coupon.id().value());
		assertThat(view.code()).isEqualTo("GET-1");
		assertThat(view.currentUsages()).isZero();
		assertThat(view.remainingUsages()).isEqualTo(5);
	}

	@Test
	void throwsWhenCouponDoesNotExist() {
		when(couponRepository.findByCode(any())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service().get("missing"))
				.isInstanceOf(CouponNotFoundException.class);
	}

	@Test
	void getsAllCoupons() {
		Coupon first = coupon("LIST-1", 5, 0, "PL");
		Coupon second = coupon("LIST-2", 3, 1, "DE");
		when(couponRepository.findAll()).thenReturn(List.of(first, second));

		List<CouponView> views = service().getAll();

		assertThat(views).extracting(CouponView::code).containsExactly("LIST-1", "LIST-2");
		assertThat(views).extracting(CouponView::remainingUsages).containsExactly(5, 2);
	}

	@Test
	void usesCouponAndPersistsUsage() {
		Coupon coupon = coupon("USE-1", 5, 0, "PL");
		when(couponRepository.findByCodeForUpdate(any())).thenReturn(Optional.of(coupon));
		when(couponUsageRepository.existsByCouponIdAndUserId(any(), any())).thenReturn(false);
		when(geoLocationPort.resolveCountry(CLIENT_IP)).thenReturn(CountryCode.of("PL"));
		when(couponRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(couponUsageRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		CouponUsageView view = service().use(new UseCouponCommand("use-1", "user-1", CLIENT_IP));

		assertThat(view.couponId()).isEqualTo(coupon.id().value());
		assertThat(view.userId()).isEqualTo("user-1");
		assertThat(view.usedAt()).isEqualTo(NOW);
		assertThat(view.country()).isEqualTo("PL");
		assertThat(coupon.currentUsages()).isEqualTo(1);
		verify(couponRepository).save(coupon);
		verify(couponUsageRepository).save(any(CouponUsage.class));
	}

	@Test
	void rejectsCouponAlreadyUsedByUser() {
		Coupon coupon = coupon("USED-1", 5, 0, "PL");
		when(couponRepository.findByCodeForUpdate(any())).thenReturn(Optional.of(coupon));
		when(couponUsageRepository.existsByCouponIdAndUserId(any(), any())).thenReturn(true);
		when(geoLocationPort.resolveCountry(CLIENT_IP)).thenReturn(CountryCode.of("PL"));

		assertThatThrownBy(() -> service().use(new UseCouponCommand("USED-1", "user-1", CLIENT_IP)))
				.isInstanceOf(CouponAlreadyUsedException.class);

		verify(couponRepository, never()).save(any());
		verify(couponUsageRepository, never()).save(any());
	}

	@Test
	void rejectsCountryMismatch() {
		Coupon coupon = coupon("COUNTRY-1", 5, 0, "PL");
		when(couponRepository.findByCodeForUpdate(any())).thenReturn(Optional.of(coupon));
		when(couponUsageRepository.existsByCouponIdAndUserId(any(), any())).thenReturn(false);
		when(geoLocationPort.resolveCountry(CLIENT_IP)).thenReturn(CountryCode.of("US"));

		assertThatThrownBy(() -> service().use(new UseCouponCommand("COUNTRY-1", "user-1", CLIENT_IP)))
				.isInstanceOf(CountryNotAllowedException.class);

		verify(couponRepository, never()).save(any());
	}

	@Test
	void rejectsExpiredCoupon() {
		Coupon coupon = restrictedCoupon("EXPIRED-1", 1, 1);
		when(couponRepository.findByCodeForUpdate(any())).thenReturn(Optional.of(coupon));
		when(couponUsageRepository.existsByCouponIdAndUserId(any(), any())).thenReturn(false);
		when(geoLocationPort.resolveCountry(CLIENT_IP)).thenReturn(CountryCode.of("PL"));

		assertThatThrownBy(() -> service().use(new UseCouponCommand("EXPIRED-1", "user-1", CLIENT_IP)))
				.isInstanceOf(CouponExpiredException.class);

		verify(couponRepository, never()).save(any());
	}

	@Test
	void rejectsMissingClientIp() {
		assertThatThrownBy(() -> service().use(new UseCouponCommand("USE-1", "user-1", " ")))
				.isInstanceOf(ClientIpNotResolvedException.class);

		verifyNoInteractions(geoLocationPort, couponRepository, couponUsageRepository);
	}

	@Test
	void rejectsGeoLocationFailure() {
		when(geoLocationPort.resolveCountry(CLIENT_IP))
				.thenThrow(new GeoLocationUnavailableException("Geo service unavailable"));

		assertThatThrownBy(() -> service().use(new UseCouponCommand("USE-1", "user-1", CLIENT_IP)))
				.isInstanceOf(GeoLocationUnavailableException.class);

		verifyNoInteractions(couponRepository, couponUsageRepository);
	}

	@Test
	void rethrowsLastOptimisticLockingFailureAfterMaxAttempts() {
		when(geoLocationPort.resolveCountry(CLIENT_IP)).thenReturn(CountryCode.of("PL"));

		AtomicInteger attempts = new AtomicInteger();
		TransactionOperations transactionOperations = new TransactionOperations() {
			@Override
			public <T> T execute(TransactionCallback<T> action) {
				throw new OptimisticLockingFailureException("stale coupon " + attempts.incrementAndGet());
			}
		};

		assertThatThrownBy(() -> service(transactionOperations)
				.use(new UseCouponCommand("RETRY-2", "user-1", CLIENT_IP)))
				.isInstanceOf(OptimisticLockingFailureException.class)
				.hasMessage("stale coupon 3");

		assertThat(attempts).hasValue(3);
		verifyNoInteractions(couponRepository, couponUsageRepository);
	}

	@Test
	void retriesAfterOptimisticLockingFailure() {
		Coupon coupon = coupon("RETRY-1", 5, 0, "PL");
		when(couponRepository.findByCodeForUpdate(any())).thenReturn(Optional.of(coupon));
		when(couponUsageRepository.existsByCouponIdAndUserId(any(), any())).thenReturn(false);
		when(geoLocationPort.resolveCountry(CLIENT_IP)).thenReturn(CountryCode.of("PL"));
		when(couponRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(couponUsageRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		AtomicInteger attempts = new AtomicInteger();
		TransactionOperations transactionOperations = new TransactionOperations() {
			@Override
			public <T> T execute(TransactionCallback<T> action) {
				if (attempts.getAndIncrement() == 0) {
					throw new OptimisticLockingFailureException("stale coupon");
				}
				return action.doInTransaction(new SimpleTransactionStatus());
			}
		};

		CouponUsageView view = service(transactionOperations)
				.use(new UseCouponCommand("RETRY-1", "user-1", CLIENT_IP));

		assertThat(view.userId()).isEqualTo("user-1");
		assertThat(attempts).hasValue(2);
		verify(couponRepository, times(1)).findByCodeForUpdate(any());
	}

	private CouponApplicationService service() {
		return service(new DirectTransactionOperations());
	}

	private CouponApplicationService service(TransactionOperations transactionOperations) {
		return new CouponApplicationService(
				couponRepository,
				couponUsageRepository,
				geoLocationPort,
				transactionOperations,
				CLOCK
		);
	}

	private Coupon coupon(String code, int maxUsages, int currentUsages, String country) {
		return Coupon.restore(
				CouponId.random(),
				CouponCode.of(code),
				NOW,
				UsageLimit.of(maxUsages),
				currentUsages,
				CountryCode.of(country),
				0
		);
	}

	private Coupon restrictedCoupon(String code, int maxUsages, int currentUsages) {
		return coupon(code, maxUsages, currentUsages, "PL");
	}

	private static final class DirectTransactionOperations implements TransactionOperations {

		@Override
		public <T> T execute(TransactionCallback<T> action) {
			return action.doInTransaction(new SimpleTransactionStatus());
		}
	}
}
