package com.medbooking.patterns.cancellation.refund;

import com.medbooking.patterns.cancellation.CancellationContext;

import java.math.BigDecimal;

/** ConcreteComponent: hoàn đủ số tiền đã thanh toán thành công, ngược lại hoàn 0. */
public class BaseRefundCalculator implements RefundCalculator {
    @Override
    public RefundQuote quote(CancellationContext context) {
        if (!context.isPaid()) {
            return RefundQuote.of(BigDecimal.ZERO, "Lịch hẹn chưa thanh toán thành công nên không có khoản hoàn.");
        }
        return RefundQuote.of(context.payment().getAmount(), "Hoàn đủ số tiền đã thanh toán.");
    }
}
