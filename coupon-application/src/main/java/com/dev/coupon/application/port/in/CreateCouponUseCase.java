package com.dev.coupon.application.port.in;

import com.dev.coupon.application.command.CreateCouponCommand;
import com.dev.coupon.application.view.CouponView;

public interface CreateCouponUseCase {

	CouponView create(CreateCouponCommand command);
}
