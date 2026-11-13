package com.medbooking.patterns.payment;

/** Adapter output independent of the external Stripe SDK. */
public record PaymentIntentInfo(String paymentIntentId, String clientSecret, long amountVnd) {}
