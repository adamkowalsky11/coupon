package com.dev.coupon.application.port.out;

import com.dev.coupon.domain.Coupon;
import com.dev.coupon.domain.CouponCode;
import com.dev.coupon.domain.CouponId;

import java.util.List;
import java.util.Optional;

public interface CouponRepository {

	Coupon save(Coupon coupon);

	List<Coupon> findAll();

	Optional<Coupon> findById(CouponId id);

	Optional<Coupon> findByCode(CouponCode code);

	Optional<Coupon> findByCodeForUpdate(CouponCode code);

	boolean existsByCode(CouponCode code);
}
