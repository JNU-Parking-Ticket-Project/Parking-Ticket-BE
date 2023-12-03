package com.jnu.ticketapi.api.coupon.controller;

import static com.jnu.ticketcommon.consts.TicketStatic.COUPON_SUCCESS_REGISTER_MESSAGE;

import com.jnu.ticketapi.api.coupon.docs.CreateCouponExceptionDocs;
import com.jnu.ticketapi.api.coupon.docs.ReadCouponExceptionDocs;
import com.jnu.ticketapi.api.coupon.model.request.CouponRegisterRequest;
import com.jnu.ticketapi.api.coupon.service.CouponRegisterUseCase;
import com.jnu.ticketapi.api.coupon.service.CouponWithDrawUseCase;
import com.jnu.ticketcommon.annotation.ApiErrorExceptionsExample;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "2. [쿠폰]")
@RequiredArgsConstructor
public class CouponController {
    private final CouponRegisterUseCase couponRegisterUseCase;
    private final CouponWithDrawUseCase couponWithDrawUseCase;

    @Operation(summary = "주차권 설정", description = "주차권 행사 세부 설정(시작일, 종료일, 잔고)")
    @ApiErrorExceptionsExample(CreateCouponExceptionDocs.class)
    @PostMapping("/coupon")
    public ResponseEntity<String> setCoupon(
            @RequestBody @Valid CouponRegisterRequest couponRegisterRequest) {
        couponRegisterUseCase.registerCoupon(couponRegisterRequest);
        return ResponseEntity.ok(COUPON_SUCCESS_REGISTER_MESSAGE);
    }

    @Operation(summary = "주차권 신청", description = "주차권 신청(주차권 신청시 잔고 감소)")
    @ApiErrorExceptionsExample(CreateCouponExceptionDocs.class)
    @PostMapping("/coupon/applicant")
    public ResponseEntity<String> issueCoupon() {
        couponWithDrawUseCase.issueCoupon();
        return ResponseEntity.ok(COUPON_SUCCESS_REGISTER_MESSAGE);
    }

    @Operation(summary = "주차권 순서 조회", description = "주차권 순서 확인")
    @ApiErrorExceptionsExample(ReadCouponExceptionDocs.class)
    @GetMapping("/coupon/order")
    public ResponseEntity<Long> getCouponOrder() {
        return ResponseEntity.ok(couponWithDrawUseCase.getCouponOrder());
    }
}
