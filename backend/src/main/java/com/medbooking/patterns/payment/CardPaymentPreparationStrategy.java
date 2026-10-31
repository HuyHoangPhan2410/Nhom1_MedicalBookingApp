package com.medbooking.patterns.payment;

import com.medbooking.entity.Payment;
import com.medbooking.repository.PaymentRepository;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;

/** Online card strategy; delegates third-party calls to the Adapter. */
@Component
public class CardPaymentPreparationStrategy implements PaymentPreparationStrategy {
    private final PaymentGateway gateway;
    private final PaymentRepository paymentRepository;

    public CardPaymentPreparationStrategy(PaymentGateway gateway, PaymentRepository paymentRepository) {
        this.gateway = gateway;
        this.paymentRepository = paymentRepository;
    }

    @Override
    public Payment.PaymentMethod method() { return Payment.PaymentMethod.card; }

    @Override
    public Map<String, Object> prepare(Payment payment) {
        Integer appointmentId = payment.getAppointment().getId();
        long amount = payment.getAmount().longValueExact();
        PaymentIntentInfo intent = gateway.createPaymentIntent(appointmentId, amount);
        payment.setStripePaymentIntentId(intent.paymentIntentId());
        payment.setPaymentMethod(Payment.PaymentMethod.card);
        paymentRepository.save(payment);

        Map<String, Object> result = new HashMap<>();
        // Keep the exact fields needed by the existing frontend.
        result.put("clientSecret", intent.clientSecret());
        result.put("paymentIntentId", intent.paymentIntentId());
        result.put("amount", intent.amountVnd());
        return result;
    }
}
