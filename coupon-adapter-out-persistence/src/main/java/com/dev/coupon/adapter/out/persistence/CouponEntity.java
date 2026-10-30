package com.dev.coupon.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
		name = "coupons",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_coupons_normalized_code",
				columnNames = "normalized_code"
		)
)
public class CouponEntity {

	@Id
	private UUID id;

	@Column(name = "normalized_code", nullable = false, length = 64)
	private String normalizedCode;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "max_usages", nullable = false)
	private int maxUsages;

	@Column(name = "current_usages", nullable = false)
	private int currentUsages;

	@Column(name = "country", nullable = false, length = 2)
	private String country;

	@Version
	@Column(name = "version", nullable = false)
	private long version;

	protected CouponEntity() {
	}

	public CouponEntity(
			UUID id,
			String normalizedCode,
			Instant createdAt,
			int maxUsages,
			int currentUsages,
			String country,
			long version
	) {
		this.id = id;
		this.normalizedCode = normalizedCode;
		this.createdAt = createdAt;
		this.maxUsages = maxUsages;
		this.currentUsages = currentUsages;
		this.country = country;
		this.version = version;
	}

	public UUID getId() {
		return id;
	}

	public String getNormalizedCode() {
		return normalizedCode;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public int getMaxUsages() {
		return maxUsages;
	}

	public int getCurrentUsages() {
		return currentUsages;
	}

	public String getCountry() {
		return country;
	}

	public long getVersion() {
		return version;
	}
}
