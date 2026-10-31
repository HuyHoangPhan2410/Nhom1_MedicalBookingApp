package com.medbooking.controller;

import com.medbooking.dto.request.ConfirmPaymentRequest;
import com.medbooking.dto.response.ApiResponse;
import com.medbooking.dto.response.PaymentResponse;
import com.medbooking.service.PaymentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Payments", description = "Payments and Stripe PaymentIntent")
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    // ✅ Giữ nguyên endpoint cũ
    @PostMapping("/confirm")
    public ApiResponse<PaymentResponse> confirmPayment(@RequestBody ConfirmPaymentRequest request) {
        PaymentResponse response = paymentService.confirmPayment(request);
        return ApiResponse.success("Xác nhận thanh toán thành công", response);
    }

    // ✅ Giữ nguyên endpoint cũ
    @GetMapping("/appointment/{appointmentId}")
    public ApiResponse<PaymentResponse> getPaymentByAppointmentId(@PathVariable Integer appointmentId) {
        PaymentResponse response = paymentService.getPaymentByAppointmentId(appointmentId);
        return ApiResponse.success(response);
    }

    // ✅ MỚI: Tạo Stripe PaymentIntent
    @PostMapping("/create-intent")
    public ApiResponse<Map<String, Object>> createPaymentIntent(@RequestParam Integer appointmentId) {
        Map<String, Object> result = paymentService.createPaymentIntent(appointmentId);
        return ApiResponse.success("Tạo PaymentIntent thành công", result);
    }

    // Strategy: preparation can select cash or card; never marks payment as successful.
    @PostMapping("/prepare")
    public ApiResponse<Map<String, Object>> preparePayment(
            @RequestParam Integer appointmentId, @RequestParam String method) {
        return ApiResponse.success("Chuẩn bị thanh toán thành công",
                paymentService.preparePayment(appointmentId, method));
    }

    // ✅ MỚI: Lấy Stripe Publishable Key
    @GetMapping("/stripe-config")
    public ApiResponse<Map<String, String>> getStripeConfig() {
        Map<String, String> config = Map.of("publishableKey", paymentService.getStripePublishableKey());
        return ApiResponse.success(config);
    }
}