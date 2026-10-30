package com.dev.coupon.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CouponUsageJpaRepository extends JpaRepository<CouponUsageEntity, UUID> {

	boolean existsByCouponIdAndUsedBy(UUID couponId, String usedBy);
}
