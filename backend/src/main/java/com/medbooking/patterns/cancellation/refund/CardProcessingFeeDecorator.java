package com.medbooking.patterns.cancellation.refund;

import com.medbooking.entity.Payment;
import com.medbooking.patterns.cancellation.CancellationContext;

import java.math.BigDecimal;

/** ConcreteDecorator: thanh toán thẻ bị trừ phí cổng thanh toán cố định, không âm. */
public class CardProcessingFeeDecorator extends RefundDecorator {
    private static final BigDecimal FEE = new BigDecimal("10000");

    public CardProcessingFeeDecorator(RefundCalculator inner) {
        super(inner);
    }

    @Override
    protected RefundQuote adjust(RefundQuote base, CancellationContext context) {
        if (context.payment().getPaymentMethod() != Payment.PaymentMethod.card) {
            return base;
        }
        BigDecimal after = base.amount().subtract(FEE).max(BigDecimal.ZERO);
        return base.adjust(after, "Thanh toán bằng thẻ: trừ phí cổng thanh toán 10.000đ.");
    }
}
