package com.dev.coupon.application.command;

public record CreateCouponCommand(String code, int maxUsages, String country) {
}
