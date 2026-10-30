package com.dev.coupon.application.port.in;

import com.dev.coupon.application.view.CouponView;

import java.util.List;

public interface GetCouponsUseCase {

	List<CouponView> getAll();
}
