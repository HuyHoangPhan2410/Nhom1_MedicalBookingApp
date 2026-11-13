package com.medbooking.service;

import com.medbooking.dto.request.ConfirmPaymentRequest;
import com.medbooking.dto.response.PaymentResponse;

import java.util.Map;

public interface PaymentService {
    PaymentResponse confirmPayment(ConfirmPaymentRequest request);
    PaymentResponse getPaymentByAppointmentId(Integer appointmentId);

    // ✅ Stripe
    Map<String, Object> createPaymentIntent(Integer appointmentId);
    Map<String, Object> preparePayment(Integer appointmentId, String method);
    String getStripePublishableKey();
}