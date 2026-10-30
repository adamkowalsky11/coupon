package com.dev.coupon.adapter.out.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CouponJpaRepository extends JpaRepository<CouponEntity, UUID> {

	List<CouponEntity> findAllByOrderByCreatedAtDesc();

	Optional<CouponEntity> findByNormalizedCode(String normalizedCode);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select coupon from CouponEntity coupon where coupon.normalizedCode = :normalizedCode")
	Optional<CouponEntity> findByNormalizedCodeForUpdate(@Param("normalizedCode") String normalizedCode);

	boolean existsByNormalizedCode(String normalizedCode);
}
