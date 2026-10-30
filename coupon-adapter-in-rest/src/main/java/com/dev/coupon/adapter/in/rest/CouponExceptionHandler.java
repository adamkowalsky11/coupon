package com.dev.coupon.adapter.in.rest;

import com.dev.coupon.application.exception.ClientIpNotResolvedException;
import com.dev.coupon.application.exception.CouponAlreadyUsedException;
import com.dev.coupon.application.exception.CouponCodeAlreadyExistsException;
import com.dev.coupon.application.exception.CouponNotFoundException;
import com.dev.coupon.application.exception.GeoLocationUnavailableException;
import com.dev.coupon.domain.CountryNotAllowedException;
import com.dev.coupon.domain.CouponExpiredException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class CouponExceptionHandler {

	@ExceptionHandler(CouponNotFoundException.class)
	ProblemDetail handleCouponNotFound(CouponNotFoundException exception) {
		return problem(HttpStatus.NOT_FOUND, "Coupon not found", exception.getMessage(), "COUPON_NOT_FOUND");
	}

	@ExceptionHandler(CouponCodeAlreadyExistsException.class)
	ProblemDetail handleCouponCodeAlreadyExists(CouponCodeAlreadyExistsException exception) {
		return problem(HttpStatus.CONFLICT, "Coupon code already exists", exception.getMessage(), "COUPON_CODE_ALREADY_EXISTS");
	}

	@ExceptionHandler(CouponAlreadyUsedException.class)
	ProblemDetail handleCouponAlreadyUsed(CouponAlreadyUsedException exception) {
		return problem(HttpStatus.CONFLICT, "Coupon already used", exception.getMessage(), "COUPON_ALREADY_USED");
	}

	@ExceptionHandler(CouponExpiredException.class)
	ProblemDetail handleCouponExpired(CouponExpiredException exception) {
		return problem(HttpStatus.CONFLICT, "Coupon expired", exception.getMessage(), "COUPON_EXPIRED");
	}

	@ExceptionHandler(CountryNotAllowedException.class)
	ProblemDetail handleCountryNotAllowed(CountryNotAllowedException exception) {
		return problem(HttpStatus.FORBIDDEN, "Country not allowed", exception.getMessage(), "COUNTRY_NOT_ALLOWED");
	}

	@ExceptionHandler(ClientIpNotResolvedException.class)
	ProblemDetail handleClientIpNotResolved(ClientIpNotResolvedException exception) {
		return problem(HttpStatus.BAD_REQUEST, "Client IP not resolved", exception.getMessage(), "CLIENT_IP_NOT_RESOLVED");
	}

	@ExceptionHandler(GeoLocationUnavailableException.class)
	ProblemDetail handleGeoLocationUnavailable(GeoLocationUnavailableException exception) {
		return problem(HttpStatus.SERVICE_UNAVAILABLE, "Geo service unavailable", exception.getMessage(), "GEO_SERVICE_UNAVAILABLE");
	}

	@ExceptionHandler(OptimisticLockingFailureException.class)
	ProblemDetail handleOptimisticLockingFailure(OptimisticLockingFailureException exception) {
		return problem(HttpStatus.CONFLICT, "Concurrent modification", "Coupon was modified concurrently", "CONCURRENT_MODIFICATION");
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException exception) {
		return problem(HttpStatus.CONFLICT, "Conflict", "Request conflicts with existing data", "CONFLICT");
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ProblemDetail handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
		String detail = exception.getBindingResult().getFieldErrors().stream()
				.map(error -> error.getField() + ": " + error.getDefaultMessage())
				.collect(Collectors.joining(", "));
		return problem(HttpStatus.BAD_REQUEST, "Validation failed", detail, "VALIDATION_FAILED");
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ProblemDetail handleHttpMessageNotReadable(HttpMessageNotReadableException exception) {
		return problem(HttpStatus.BAD_REQUEST, "Invalid request", "Request body is missing or malformed", "INVALID_REQUEST");
	}

	@ExceptionHandler(IllegalArgumentException.class)
	ProblemDetail handleIllegalArgument(IllegalArgumentException exception) {
		return problem(HttpStatus.BAD_REQUEST, "Validation failed", exception.getMessage(), "VALIDATION_FAILED");
	}

	private ProblemDetail problem(HttpStatus status, String title, String detail, String code) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setTitle(title);
		problem.setProperty("code", code);
		return problem;
	}
}
