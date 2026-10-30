package com.dev.coupon;

import com.dev.coupon.application.port.out.CouponRepository;
import com.dev.coupon.application.port.out.CouponUsageRepository;
import com.dev.coupon.domain.CountryCode;
import com.dev.coupon.domain.Coupon;
import com.dev.coupon.domain.CouponCode;
import com.dev.coupon.domain.CouponUsage;
import com.dev.coupon.domain.UsageLimit;
import com.dev.coupon.domain.UserId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class CouponPersistenceAdapterIntegrationTests {

	private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

	@Autowired
	private CouponRepository couponRepository;

	@Autowired
	private CouponUsageRepository couponUsageRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void flywayCreatesSchema() {
		assertThat(jdbcTemplate.queryForObject(
				"SELECT to_regclass('public.coupons')::text",
				String.class
		)).isEqualTo("coupons");
		assertThat(jdbcTemplate.queryForObject(
				"SELECT to_regclass('public.coupon_usages')::text",
				String.class
		)).isEqualTo("coupon_usages");
		assertThat(jdbcTemplate.queryForObject(
				"SELECT count(*) FROM flyway_schema_history WHERE success",
				Long.class
		)).isPositive();
	}

	@Test
	void savesAndFindsCouponByNormalizedCode() {
		Coupon coupon = Coupon.create(
				CouponCode.of("spring-2026"),
				UsageLimit.of(5),
				CountryCode.of("pl"),
				NOW
		);

		Coupon saved = couponRepository.save(coupon);

		assertThat(saved.id()).isEqualTo(coupon.id());
		assertThat(saved.code().value()).isEqualTo("SPRING-2026");
		assertThat(saved.createdAt()).isEqualTo(NOW);
		assertThat(saved.maxUsages().value()).isEqualTo(5);
		assertThat(saved.currentUsages()).isZero();
		assertThat(saved.country().value()).isEqualTo("PL");
		assertThat(saved.version()).isZero();
		assertThat(couponRepository.findById(saved.id())).contains(saved);
		assertThat(couponRepository.findByCode(CouponCode.of("SPRING-2026"))).contains(saved);
		assertThat(couponRepository.existsByCode(CouponCode.of("spring-2026"))).isTrue();
	}

	@Test
	void findsAllCouponsNewestFirst() {
		Coupon older = couponRepository.save(Coupon.create(
				CouponCode.of("OLDER-1"),
				UsageLimit.of(5),
				CountryCode.of("PL"),
				NOW.minusSeconds(60)
		));
		Coupon newer = couponRepository.save(Coupon.create(
				CouponCode.of("NEWER-1"),
				UsageLimit.of(5),
				CountryCode.of("PL"),
				NOW
		));

		assertThat(couponRepository.findAll())
				.extracting(coupon -> coupon.code().value())
				.containsExactly(newer.code().value(), older.code().value());
	}

	@Test
	void updatesCouponAndIncrementsVersion() {
		Coupon saved = couponRepository.save(Coupon.create(
				CouponCode.of("UPDATE-1"),
				UsageLimit.of(2),
				CountryCode.of("PL"),
				NOW
		));
		CouponUsage usage = saved.use(UserId.of("user-1"), CountryCode.of("PL"), NOW);

		Coupon updated = couponRepository.save(saved);
		couponUsageRepository.save(usage);

		assertThat(updated.currentUsages()).isEqualTo(1);
		assertThat(updated.remainingUsages()).isEqualTo(1);
		assertThat(updated.version()).isGreaterThan(saved.version());

		Coupon persisted = couponRepository.findById(saved.id()).orElseThrow();
		assertThat(persisted.currentUsages()).isEqualTo(1);
		assertThat(persisted.version()).isEqualTo(updated.version());
	}

	@Test
	void persistsUsageAndDetectsDuplicateUser() {
		Coupon coupon = couponRepository.save(Coupon.create(
				CouponCode.of("USAGE-1"),
				UsageLimit.of(5),
				CountryCode.of("PL"),
				NOW
		));
		UserId userId = UserId.of("user-1");
		CouponUsage usage = CouponUsage.create(coupon.id(), userId, NOW, CountryCode.of("PL"));

		CouponUsage saved = couponUsageRepository.save(usage);

		assertThat(saved.id()).isEqualTo(usage.id());
		assertThat(saved.couponId()).isEqualTo(coupon.id());
		assertThat(saved.userId()).isEqualTo(userId);
		assertThat(saved.usedAt()).isEqualTo(NOW);
		assertThat(saved.country().value()).isEqualTo("PL");
		assertThat(couponUsageRepository.existsByCouponIdAndUserId(coupon.id(), userId)).isTrue();
		assertThat(jdbcTemplate.queryForObject(
				"SELECT used_by FROM coupon_usages WHERE coupon_id = ?",
				String.class,
				coupon.id().value()
		)).isEqualTo("user-1");

		CouponUsage duplicate = CouponUsage.create(coupon.id(), userId, NOW, CountryCode.of("PL"));

		assertThatThrownBy(() -> couponUsageRepository.save(duplicate))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void rejectsDuplicateCouponCode() {
		couponRepository.save(Coupon.create(
				CouponCode.of("DUP-1"),
				UsageLimit.of(5),
				CountryCode.of("PL"),
				NOW
		));

		Coupon duplicate = Coupon.create(
				CouponCode.of("dup-1"),
				UsageLimit.of(5),
				CountryCode.of("PL"),
				NOW
		);

		assertThatThrownBy(() -> couponRepository.save(duplicate))
				.isInstanceOf(DataIntegrityViolationException.class);
	}
}
