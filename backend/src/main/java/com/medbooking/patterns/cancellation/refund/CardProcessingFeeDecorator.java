package com.medbooking.patterns.cancellation.refund;

import com.medbooking.entity.Payment;
import com.medbooking.patterns.cancellation.CancellationContext;
import java.math.BigDecimal;

public final class CardProcessingFeeDecorator extends RefundDecorator {
    private static final BigDecimal FEE = new BigDecimal("10000");

    public CardProcessingFeeDecorator(RefundCalculator inner) { super(inner); }

    @Override protected RefundQuote adjust(RefundQuote base, CancellationContext context) {
        return context.isPaid() && context.payment().getPaymentMethod() == Payment.PaymentMethod.card
                ? base.adjust(base.amount().subtract(FEE), "Thanh toán thẻ: trừ phí xử lý tối đa 10.000đ, không vượt số tiền còn lại")
                : base;
    }
}
