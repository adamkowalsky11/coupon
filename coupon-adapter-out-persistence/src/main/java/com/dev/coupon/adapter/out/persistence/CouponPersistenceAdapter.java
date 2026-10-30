package com.dev.coupon.adapter.out.persistence;

import com.dev.coupon.application.port.out.CouponRepository;
import com.dev.coupon.application.port.out.CouponUsageRepository;
import com.dev.coupon.domain.CountryCode;
import com.dev.coupon.domain.Coupon;
import com.dev.coupon.domain.CouponCode;
import com.dev.coupon.domain.CouponId;
import com.dev.coupon.domain.CouponUsage;
import com.dev.coupon.domain.CouponUsageId;
import com.dev.coupon.domain.UsageLimit;
import com.dev.coupon.domain.UserId;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class CouponPersistenceAdapter implements CouponRepository, CouponUsageRepository {

	private final CouponJpaRepository couponJpaRepository;
	private final CouponUsageJpaRepository couponUsageJpaRepository;

	public CouponPersistenceAdapter(
			CouponJpaRepository couponJpaRepository,
			CouponUsageJpaRepository couponUsageJpaRepository
	) {
		this.couponJpaRepository = couponJpaRepository;
		this.couponUsageJpaRepository = couponUsageJpaRepository;
	}

	@Override
	public Coupon save(Coupon coupon) {
		CouponEntity saved = couponJpaRepository.saveAndFlush(toEntity(coupon));
		return toDomain(saved);
	}

	@Override
	public List<Coupon> findAll() {
		return couponJpaRepository.findAllByOrderByCreatedAtDesc().stream()
				.map(this::toDomain)
				.toList();
	}

	@Override
	public Optional<Coupon> findById(CouponId id) {
		return couponJpaRepository.findById(id.value()).map(this::toDomain);
	}

	@Override
	public Optional<Coupon> findByCode(CouponCode code) {
		return couponJpaRepository.findByNormalizedCode(code.value()).map(this::toDomain);
	}

	@Override
	public Optional<Coupon> findByCodeForUpdate(CouponCode code) {
		return couponJpaRepository.findByNormalizedCodeForUpdate(code.value()).map(this::toDomain);
	}

	@Override
	public boolean existsByCode(CouponCode code) {
		return couponJpaRepository.existsByNormalizedCode(code.value());
	}

	@Override
	public CouponUsage save(CouponUsage usage) {
		CouponUsageEntity saved = couponUsageJpaRepository.saveAndFlush(toEntity(usage));
		return toDomain(saved);
	}

	@Override
	public boolean existsByCouponIdAndUserId(CouponId couponId, UserId userId) {
		return couponUsageJpaRepository.existsByCouponIdAndUsedBy(couponId.value(), userId.value());
	}

	private CouponEntity toEntity(Coupon coupon) {
		return new CouponEntity(
				coupon.id().value(),
				coupon.code().value(),
				coupon.createdAt(),
				coupon.maxUsages().value(),
				coupon.currentUsages(),
				coupon.country().value(),
				coupon.version()
		);
	}

	private Coupon toDomain(CouponEntity entity) {
		return Coupon.restore(
				CouponId.of(entity.getId()),
				CouponCode.of(entity.getNormalizedCode()),
				entity.getCreatedAt(),
				UsageLimit.of(entity.getMaxUsages()),
				entity.getCurrentUsages(),
				CountryCode.of(entity.getCountry()),
				entity.getVersion()
		);
	}

	private CouponUsageEntity toEntity(CouponUsage usage) {
		return new CouponUsageEntity(
				usage.id().value(),
				usage.couponId().value(),
				usage.userId().value(),
				usage.usedAt(),
				usage.country().value()
		);
	}

	private CouponUsage toDomain(CouponUsageEntity entity) {
		return CouponUsage.restore(
				CouponUsageId.of(entity.getId()),
				CouponId.of(entity.getCouponId()),
				UserId.of(entity.getUsedBy()),
				entity.getUsedAt(),
				CountryCode.of(entity.getCountry())
		);
	}
}
