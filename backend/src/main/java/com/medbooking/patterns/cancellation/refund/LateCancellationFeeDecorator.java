package com.medbooking.patterns.cancellation.refund;

import com.medbooking.patterns.cancellation.CancellationContext;

import java.math.BigDecimal;
import java.time.Duration;

/** ConcreteDecorator: hủy trong vòng 24 giờ trước giờ khám thì khấu trừ 30%. */
public class LateCancellationFeeDecorator extends RefundDecorator {
    private static final Duration LATE_WINDOW = Duration.ofHours(24);
    private static final BigDecimal KEEP_RATE = new BigDecimal("0.70");

    public LateCancellationFeeDecorator(RefundCalculator inner) {
        super(inner);
    }

    @Override
    protected RefundQuote adjust(RefundQuote base, CancellationContext context) {
        if (context.timeUntilStart().compareTo(LATE_WINDOW) >= 0) {
            return base;
        }
        return base.adjust(base.amount().multiply(KEEP_RATE),
                "Hủy trong vòng 24 giờ trước giờ khám: khấu trừ 30%.");
    }
}
