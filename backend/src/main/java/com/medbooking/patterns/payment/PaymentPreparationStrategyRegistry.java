package com.medbooking.patterns.payment;

import com.medbooking.entity.Payment;
import com.medbooking.exception.BusinessException;
import org.springframework.stereotype.Component;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Runtime selection of payment preparation algorithms. */
@Component
public class PaymentPreparationStrategyRegistry {
    private final Map<Payment.PaymentMethod, PaymentPreparationStrategy> strategies;

    public PaymentPreparationStrategyRegistry(List<PaymentPreparationStrategy> available) {
        strategies = new EnumMap<>(Payment.PaymentMethod.class);
        for (PaymentPreparationStrategy strategy : available) {
            if (strategies.putIfAbsent(strategy.method(), strategy) != null) {
                throw new IllegalArgumentException("Duplicate payment strategy: " + strategy.method());
            }
        }
    }

    public PaymentPreparationStrategy forMethod(Payment.PaymentMethod method) {
        PaymentPreparationStrategy strategy = strategies.get(method);
        if (strategy == null) {
            throw new BusinessException(400, "Phương thức thanh toán không được hỗ trợ: " + method);
        }
        return strategy;
    }
}
