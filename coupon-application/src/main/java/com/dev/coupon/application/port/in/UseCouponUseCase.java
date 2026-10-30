package com.dev.coupon.application.port.in;

import com.dev.coupon.application.command.UseCouponCommand;
import com.dev.coupon.application.view.CouponUsageView;

public interface UseCouponUseCase {

	CouponUsageView use(UseCouponCommand command);
}
