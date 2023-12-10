package com.jnu.ticketapi.api.coupon.controller;

import static com.jnu.ticketcommon.message.ResponseMessage.COUPON_SUCCESS_REGISTER_MESSAGE;

import com.jnu.ticketapi.api.coupon.docs.CreateCouponExceptionDocs;
import com.jnu.ticketapi.api.coupon.docs.ReadCouponExceptionDocs;
import com.jnu.ticketapi.api.coupon.service.CouponRegisterUseCase;
import com.jnu.ticketapi.api.coupon.service.CouponWithDrawUseCase;
import com.jnu.ticketcommon.annotation.ApiErrorExceptionsExample;
import com.jnu.ticketcommon.dto.SuccessResponse;
import com.jnu.ticketdomain.common.vo.DateTimePeriod;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "2. [쿠폰]")
@RequiredArgsConstructor
@SecurityRequirement(name = "access-token")
@RequestMapping("/v1")
public class CouponController {

    private final CouponRegisterUseCase couponRegisterUseCase;
    private final CouponWithDrawUseCase couponWithDrawUseCase;

    @Operation(summary = "주차권 설정", description = "주차권 행사 세부 설정(시작일, 종료일, 잔고)")
    @ApiErrorExceptionsExample(CreateCouponExceptionDocs.class)
    @PostMapping("/coupons")
    public SuccessResponse setCoupon(@RequestBody DateTimePeriod dateTimePeriod) {
        couponRegisterUseCase.registerCoupon(dateTimePeriod);
        return new SuccessResponse(COUPON_SUCCESS_REGISTER_MESSAGE);
    }

    @Operation(summary = "주차권 신청", description = "주차권 신청(주차권 신청시 잔고 감소)")
    @Deprecated(since = "2023-12-08", forRemoval = true)
    @PostMapping("/coupons/apply")
    public SuccessResponse issueCoupon() {
        //        couponWithDrawUseCase.issueCoupon();
        return new SuccessResponse(COUPON_SUCCESS_REGISTER_MESSAGE);
    }

    @Operation(summary = "현재 대기번호 조회", description = "주차권 대기번호 조회")
    @ApiErrorExceptionsExample(ReadCouponExceptionDocs.class)
    @GetMapping("/coupons/order")
    public ResponseEntity<Long> getCouponOrder() {
        return ResponseEntity.ok(couponWithDrawUseCase.getCouponOrder());
    }

    @Operation(summary = "주차권 신청 기간 조회", description = "주차권 신청 기간 조회")
    @ApiErrorExceptionsExample(ReadCouponExceptionDocs.class)
    @GetMapping("/coupons/period")
    public ResponseEntity<DateTimePeriod> getCouponPeriod() {
        return ResponseEntity.ok(couponWithDrawUseCase.getCouponPeriod());
    }
}
