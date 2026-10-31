package com.medbooking.patterns.payment;

/** Our domain-facing gateway; no Stripe types escape this interface. */
public interface PaymentGateway {
    PaymentIntentInfo createPaymentIntent(Integer appointmentId, long amountVnd);
}
