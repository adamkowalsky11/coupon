package com.dev.coupon.application.service;

import com.dev.coupon.application.command.CreateCouponCommand;
import com.dev.coupon.application.command.UseCouponCommand;
import com.dev.coupon.application.exception.ClientIpNotResolvedException;
import com.dev.coupon.application.exception.CouponAlreadyUsedException;
import com.dev.coupon.application.exception.CouponCodeAlreadyExistsException;
import com.dev.coupon.application.exception.CouponNotFoundException;
import com.dev.coupon.application.port.in.CreateCouponUseCase;
import com.dev.coupon.application.port.in.GetCouponUseCase;
import com.dev.coupon.application.port.in.GetCouponsUseCase;
import com.dev.coupon.application.port.in.UseCouponUseCase;
import com.dev.coupon.application.port.out.CouponRepository;
import com.dev.coupon.application.port.out.CouponUsageRepository;
import com.dev.coupon.application.port.out.GeoLocationPort;
import com.dev.coupon.application.view.CouponUsageView;
import com.dev.coupon.application.view.CouponView;
import com.dev.coupon.domain.CountryCode;
import com.dev.coupon.domain.Coupon;
import com.dev.coupon.domain.CouponCode;
import com.dev.coupon.domain.CouponUsage;
import com.dev.coupon.domain.UsageLimit;
import com.dev.coupon.domain.UserId;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.support.TransactionOperations;

import java.time.Clock;
import java.util.List;
import java.util.Objects;

public class CouponApplicationService implements CreateCouponUseCase, GetCouponUseCase, GetCouponsUseCase, UseCouponUseCase {

	private static final int MAX_ATTEMPTS = 3;

	private final CouponRepository couponRepository;
	private final CouponUsageRepository couponUsageRepository;
	private final GeoLocationPort geoLocationPort;
	private final TransactionOperations transactionOperations;
	private final Clock clock;

	public CouponApplicationService(
			CouponRepository couponRepository,
			CouponUsageRepository couponUsageRepository,
			GeoLocationPort geoLocationPort,
			TransactionOperations transactionOperations,
			Clock clock
	) {
		this.couponRepository = couponRepository;
		this.couponUsageRepository = couponUsageRepository;
		this.geoLocationPort = geoLocationPort;
		this.transactionOperations = transactionOperations;
		this.clock = clock;
	}

	@Override
	public CouponView create(CreateCouponCommand command) {
		CouponCode code = CouponCode.of(command.code());
		UsageLimit maxUsages = UsageLimit.of(command.maxUsages());
		CountryCode country = CountryCode.of(command.country());

		return requireResult(transactionOperations.execute(status -> {
			if (couponRepository.existsByCode(code)) {
				throw new CouponCodeAlreadyExistsException(code);
			}

			Coupon coupon = Coupon.create(code, maxUsages, country, clock.instant());
			return toView(couponRepository.save(coupon));
		}));
	}

	@Override
	public CouponView get(String code) {
		CouponCode couponCode = CouponCode.of(code);
		return couponRepository.findByCode(couponCode)
				.map(this::toView)
				.orElseThrow(() -> new CouponNotFoundException(couponCode.value()));
	}

	@Override
	public List<CouponView> getAll() {
		return couponRepository.findAll().stream()
				.map(this::toView)
				.toList();
	}

	@Override
	public CouponUsageView use(UseCouponCommand command) {
		CouponCode code = CouponCode.of(command.code());
		UserId userId = UserId.of(command.userId());
		CountryCode country = geoLocationPort.resolveCountry(resolveClientIp(command.clientIp()));

		OptimisticLockingFailureException lastFailure = null;

		for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
			try {
				return requireResult(transactionOperations.execute(
						_ -> useInTransaction(code, userId, country)
				));
			} catch (OptimisticLockingFailureException exception) {
				lastFailure = exception;
			}
		}

		throw Objects.requireNonNull(lastFailure, "Retry loop exited without an exception");
	}

	private CouponUsageView useInTransaction(CouponCode code, UserId userId, CountryCode country) {
		Coupon coupon = couponRepository.findByCodeForUpdate(code)
				.orElseThrow(() -> new CouponNotFoundException(code.value()));

		if (couponUsageRepository.existsByCouponIdAndUserId(coupon.id(), userId)) {
			throw new CouponAlreadyUsedException(coupon.id(), userId);
		}

		CouponUsage usage = coupon.use(userId, country, clock.instant());
		couponRepository.save(coupon);
		return toView(couponUsageRepository.save(usage));
	}

	private String resolveClientIp(String clientIp) {
		if (clientIp == null || clientIp.isBlank()) {
			throw new ClientIpNotResolvedException(clientIp);
		}
		return clientIp.trim();
	}

	private CouponView toView(Coupon coupon) {
		return new CouponView(
				coupon.id().value(),
				coupon.code().value(),
				coupon.maxUsages().value(),
				coupon.currentUsages(),
				coupon.remainingUsages(),
				coupon.country().value(),
				coupon.createdAt()
		);
	}

	private CouponUsageView toView(CouponUsage usage) {
		return new CouponUsageView(
				usage.id().value(),
				usage.couponId().value(),
				usage.userId().value(),
				usage.usedAt(),
				usage.country().value()
		);
	}

	private <T> T requireResult(T result) {
		return Objects.requireNonNull(result, "Transaction returned no result");
	}
}
