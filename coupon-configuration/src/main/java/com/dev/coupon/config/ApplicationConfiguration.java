package com.dev.coupon.config;

import com.dev.coupon.application.port.out.CouponRepository;
import com.dev.coupon.application.port.out.CouponUsageRepository;
import com.dev.coupon.application.port.out.GeoLocationPort;
import com.dev.coupon.application.service.CouponApplicationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;

@Configuration
public class ApplicationConfiguration {

	@Bean
	TransactionOperations couponTransactionOperations(PlatformTransactionManager transactionManager) {
		return new TransactionTemplate(transactionManager);
	}

	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}

	@Bean
	CouponApplicationService couponApplicationService(
			CouponRepository couponRepository,
			CouponUsageRepository couponUsageRepository,
			GeoLocationPort geoLocationPort,
			TransactionOperations couponTransactionOperations,
			Clock clock
	) {
		return new CouponApplicationService(
				couponRepository,
				couponUsageRepository,
				geoLocationPort,
				couponTransactionOperations,
				clock
		);
	}
}
