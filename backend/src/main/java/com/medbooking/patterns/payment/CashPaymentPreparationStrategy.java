package com.medbooking.patterns.payment;

import com.medbooking.entity.Payment;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;

/** No online charge; the payment remains pending until the clinic confirms it. */
@Component
public class CashPaymentPreparationStrategy implements PaymentPreparationStrategy {
    @Override
    public Payment.PaymentMethod method() { return Payment.PaymentMethod.cash; }

    @Override
    public Map<String, Object> prepare(Payment payment) {
        Map<String, Object> result = new HashMap<>();
        result.put("appointmentId", payment.getAppointment().getId());
        result.put("amount", payment.getAmount().longValue());
        result.put("paymentMethod", "cash");
        result.put("status", "pending");
        result.put("instructions", "Thanh toán tại phòng khám trước khi khám");
        return result;
    }
}
