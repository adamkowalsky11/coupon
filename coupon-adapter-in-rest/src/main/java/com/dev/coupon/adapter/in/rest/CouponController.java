package com.dev.coupon.adapter.in.rest;

import com.dev.coupon.application.command.CreateCouponCommand;
import com.dev.coupon.application.command.UseCouponCommand;
import com.dev.coupon.application.port.in.CreateCouponUseCase;
import com.dev.coupon.application.port.in.GetCouponUseCase;
import com.dev.coupon.application.port.in.GetCouponsUseCase;
import com.dev.coupon.application.port.in.UseCouponUseCase;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/coupons")
public class CouponController {

	private final CreateCouponUseCase createCouponUseCase;
	private final GetCouponUseCase getCouponUseCase;
	private final GetCouponsUseCase getCouponsUseCase;
	private final UseCouponUseCase useCouponUseCase;

	public CouponController(
			CreateCouponUseCase createCouponUseCase,
			GetCouponUseCase getCouponUseCase,
			GetCouponsUseCase getCouponsUseCase,
			UseCouponUseCase useCouponUseCase
	) {
		this.createCouponUseCase = createCouponUseCase;
		this.getCouponUseCase = getCouponUseCase;
		this.getCouponsUseCase = getCouponsUseCase;
		this.useCouponUseCase = useCouponUseCase;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public CouponResponse create(@Valid @RequestBody CreateCouponRequest request) {
		return CouponResponse.from(createCouponUseCase.create(new CreateCouponCommand(
				request.code(),
				request.maxUsages(),
				request.country()
		)));
	}

	@GetMapping
	public List<CouponResponse> getAll() {
		return getCouponsUseCase.getAll().stream()
				.map(CouponResponse::from)
				.toList();
	}

	@GetMapping("/{code}")
	public CouponResponse get(@PathVariable("code") String code) {
		return CouponResponse.from(getCouponUseCase.get(code));
	}

	@PostMapping("/{code}/couponusage")
	@ResponseStatus(HttpStatus.CREATED)
	public CouponUsageResponse use(
			@PathVariable("code") String code,
			@Valid @RequestBody UseCouponRequest request,
			@Parameter(description = "Public client IP used for geolocation", example = "83.16.0.1")
			@RequestHeader(value = "X-Forwarded-For", required = false) String forwardedFor,
			HttpServletRequest httpRequest
	) {
		return CouponUsageResponse.from(useCouponUseCase.use(new UseCouponCommand(
				code,
				request.userId(),
				resolveClientIp(forwardedFor, httpRequest)
		)));
	}

	private String resolveClientIp(String forwardedFor, HttpServletRequest request) {
		if (forwardedFor != null) {
			for (String candidate : forwardedFor.split(",")) {
				if (!candidate.isBlank()) {
					return candidate.trim();
				}
			}
		}
		return request.getRemoteAddr();
	}
}
