package com.dev.coupon.application.command;

public record UseCouponCommand(String code, String userId, String clientIp) {
}
