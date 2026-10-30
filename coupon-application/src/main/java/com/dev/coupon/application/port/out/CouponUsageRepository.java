package com.dev.coupon.application.port.out;

import com.dev.coupon.domain.CouponId;
import com.dev.coupon.domain.CouponUsage;
import com.dev.coupon.domain.UserId;

public interface CouponUsageRepository {

	CouponUsage save(CouponUsage usage);

	boolean existsByCouponIdAndUserId(CouponId couponId, UserId userId);
}
