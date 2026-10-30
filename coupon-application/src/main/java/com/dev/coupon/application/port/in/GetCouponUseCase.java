package com.dev.coupon.application.port.in;

import com.dev.coupon.application.view.CouponView;

public interface GetCouponUseCase {

	CouponView get(String code);
}
