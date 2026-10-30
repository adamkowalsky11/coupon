package com.dev.coupon.adapter.in.rest;

import com.dev.coupon.application.command.CreateCouponCommand;
import com.dev.coupon.application.command.UseCouponCommand;
import com.dev.coupon.application.exception.CouponCodeAlreadyExistsException;
import com.dev.coupon.application.exception.CouponNotFoundException;
import com.dev.coupon.application.exception.GeoLocationUnavailableException;
import com.dev.coupon.application.port.in.CreateCouponUseCase;
import com.dev.coupon.application.port.in.GetCouponUseCase;
import com.dev.coupon.application.port.in.GetCouponsUseCase;
import com.dev.coupon.application.port.in.UseCouponUseCase;
import com.dev.coupon.application.view.CouponUsageView;
import com.dev.coupon.application.view.CouponView;
import com.dev.coupon.domain.CountryCode;
import com.dev.coupon.domain.CountryNotAllowedException;
import com.dev.coupon.domain.CouponCode;
import com.dev.coupon.domain.CouponExpiredException;
import com.dev.coupon.domain.CouponId;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CouponController.class)
class CouponControllerWebMvcTests {

	private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private CreateCouponUseCase createCouponUseCase;

	@MockitoBean
	private GetCouponUseCase getCouponUseCase;

	@MockitoBean
	private GetCouponsUseCase getCouponsUseCase;

	@MockitoBean
	private UseCouponUseCase useCouponUseCase;

	@Test
	void createsCoupon() throws Exception {
		when(createCouponUseCase.create(any())).thenReturn(couponView());

		mockMvc.perform(post("/api/v1/coupons")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "code": "spring-2026",
								  "maxUsages": 5,
								  "country": "pl"
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.id").value("11111111-1111-1111-1111-111111111111"))
				.andExpect(jsonPath("$.code").value("SPRING-2026"))
				.andExpect(jsonPath("$.maxUsages").value(5))
				.andExpect(jsonPath("$.currentUsages").value(0))
				.andExpect(jsonPath("$.remainingUsages").value(5))
				.andExpect(jsonPath("$.country").value("PL"));

		ArgumentCaptor<CreateCouponCommand> command = ArgumentCaptor.forClass(CreateCouponCommand.class);
		verify(createCouponUseCase).create(command.capture());
		assertThat(command.getValue().code()).isEqualTo("spring-2026");
		assertThat(command.getValue().maxUsages()).isEqualTo(5);
		assertThat(command.getValue().country()).isEqualTo("pl");
	}

	@Test
	void rejectsInvalidCreateRequest() throws Exception {
		mockMvc.perform(post("/api/v1/coupons")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "code": "",
								  "maxUsages": 0,
								  "country": ""
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

		verifyNoInteractions(createCouponUseCase);
	}

	@Test
	void getsAllCoupons() throws Exception {
		when(getCouponsUseCase.getAll()).thenReturn(List.of(couponView()));

		mockMvc.perform(get("/api/v1/coupons"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$[0].code").value("SPRING-2026"))
				.andExpect(jsonPath("$[0].remainingUsages").value(5));

		verify(getCouponsUseCase).getAll();
	}

	@Test
	void getsCoupon() throws Exception {
		when(getCouponUseCase.get("spring-2026")).thenReturn(couponView());

		mockMvc.perform(get("/api/v1/coupons/spring-2026"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value("SPRING-2026"))
				.andExpect(jsonPath("$.remainingUsages").value(5));
	}

	@Test
	void returnsNotFoundWhenCouponDoesNotExist() throws Exception {
		when(getCouponUseCase.get("missing")).thenThrow(new CouponNotFoundException("MISSING"));

		mockMvc.perform(get("/api/v1/coupons/missing"))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("COUPON_NOT_FOUND"))
				.andExpect(jsonPath("$.detail").value("Coupon MISSING not found"));
	}

	@Test
	void usesCouponAndReadsFirstForwardedIp() throws Exception {
		when(useCouponUseCase.use(any())).thenReturn(couponUsageView());

		mockMvc.perform(post("/api/v1/coupons/spring-2026/couponusage")
						.header("X-Forwarded-For", "203.0.113.10, 10.0.0.1")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "userId": "user-1"
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.userId").value("user-1"))
				.andExpect(jsonPath("$.country").value("PL"));

		ArgumentCaptor<UseCouponCommand> command = ArgumentCaptor.forClass(UseCouponCommand.class);
		verify(useCouponUseCase).use(command.capture());
		assertThat(command.getValue().code()).isEqualTo("spring-2026");
		assertThat(command.getValue().userId()).isEqualTo("user-1");
		assertThat(command.getValue().clientIp()).isEqualTo("203.0.113.10");
	}

	@Test
	void usesRemoteAddressWhenForwardedHeaderIsMissing() throws Exception {
		when(useCouponUseCase.use(any())).thenReturn(couponUsageView());

		mockMvc.perform(post("/api/v1/coupons/spring-2026/couponusage")
						.with(request -> {
							request.setRemoteAddr("198.51.100.7");
							return request;
						})
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "userId": "user-1"
								}
								"""))
				.andExpect(status().isCreated());

		ArgumentCaptor<UseCouponCommand> command = ArgumentCaptor.forClass(UseCouponCommand.class);
		verify(useCouponUseCase).use(command.capture());
		assertThat(command.getValue().clientIp()).isEqualTo("198.51.100.7");
	}

	@Test
	void rejectsInvalidUsageRequest() throws Exception {
		mockMvc.perform(post("/api/v1/coupons/spring-2026/couponusage")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "userId": ""
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

		verifyNoInteractions(useCouponUseCase);
	}

	@Test
	void returnsConflictWhenCouponIsExpired() throws Exception {
		when(useCouponUseCase.use(any())).thenThrow(new CouponExpiredException(CouponId.random(), 5, 5));

		mockMvc.perform(post("/api/v1/coupons/spring-2026/couponusage")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "userId": "user-1"
								}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("COUPON_EXPIRED"));
	}

	@Test
	void returnsForbiddenWhenCountryIsNotAllowed() throws Exception {
		when(useCouponUseCase.use(any())).thenThrow(new CountryNotAllowedException(
				CouponId.random(),
				CountryCode.of("PL"),
				CountryCode.of("US")
		));

		mockMvc.perform(post("/api/v1/coupons/spring-2026/couponusage")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "userId": "user-1"
								}
								"""))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("COUNTRY_NOT_ALLOWED"));
	}

	@Test
	void returnsServiceUnavailableWhenGeoServiceFails() throws Exception {
		when(useCouponUseCase.use(any())).thenThrow(new GeoLocationUnavailableException("Geo service unavailable"));

		mockMvc.perform(post("/api/v1/coupons/spring-2026/couponusage")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "userId": "user-1"
								}
								"""))
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.code").value("GEO_SERVICE_UNAVAILABLE"));
	}

	@Test
	void returnsConflictWhenCodeAlreadyExists() throws Exception {
		when(createCouponUseCase.create(any()))
				.thenThrow(new CouponCodeAlreadyExistsException(CouponCode.of("DUP-1")));

		mockMvc.perform(post("/api/v1/coupons")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "code": "DUP-1",
								  "maxUsages": 5,
								  "country": "PL"
								}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("COUPON_CODE_ALREADY_EXISTS"));
	}

	@Test
	void returnsBadRequestForMalformedJson() throws Exception {
		mockMvc.perform(post("/api/v1/coupons")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"code\":"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
	}

	private CouponView couponView() {
		return new CouponView(
				UUID.fromString("11111111-1111-1111-1111-111111111111"),
				"SPRING-2026",
				5,
				0,
				5,
				"PL",
				NOW
		);
	}

	private CouponUsageView couponUsageView() {
		return new CouponUsageView(
				UUID.fromString("22222222-2222-2222-2222-222222222222"),
				UUID.fromString("11111111-1111-1111-1111-111111111111"),
				"user-1",
				NOW,
				"PL"
		);
	}
}
