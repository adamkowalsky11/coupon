package com.dev.coupon.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
		name = "coupon_usages",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_coupon_usages_coupon_user",
				columnNames = {"coupon_id", "used_by"}
		)
)
public class CouponUsageEntity {

	@Id
	private UUID id;

	@Column(name = "coupon_id", nullable = false)
	private UUID couponId;

	@Column(name = "used_by", nullable = false, length = 128)
	private String usedBy;

	@Column(name = "used_at", nullable = false)
	private Instant usedAt;

	@Column(name = "country", nullable = false, length = 2)
	private String country;

	protected CouponUsageEntity() {
	}

	public CouponUsageEntity(UUID id, UUID couponId, String usedBy, Instant usedAt, String country) {
		this.id = id;
		this.couponId = couponId;
		this.usedBy = usedBy;
		this.usedAt = usedAt;
		this.country = country;
	}

	public UUID getId() {
		return id;
	}

	public UUID getCouponId() {
		return couponId;
	}

	public String getUsedBy() {
		return usedBy;
	}

	public Instant getUsedAt() {
		return usedAt;
	}

	public String getCountry() {
		return country;
	}
}
