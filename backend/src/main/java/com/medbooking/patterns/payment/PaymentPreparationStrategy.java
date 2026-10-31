package com.medbooking.patterns.payment;

import com.medbooking.entity.Payment;
import java.util.Map;

/** Strategy (Behavioral): prepare different payment methods. */
public interface PaymentPreparationStrategy {
    Payment.PaymentMethod method();
    Map<String, Object> prepare(Payment payment);
}
