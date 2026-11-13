package com.medbooking.patterns.cancellation.refund;

import com.medbooking.patterns.cancellation.CancellationContext;
import java.math.BigDecimal;
import java.time.Duration;

public final class LateCancellationFeeDecorator extends RefundDecorator {
    private static final Duration LATE_WINDOW = Duration.ofHours(24);
    private static final BigDecimal KEEP_RATE = new BigDecimal("0.70");

    public LateCancellationFeeDecorator(RefundCalculator inner) { super(inner); }

    @Override protected RefundQuote adjust(RefundQuote base, CancellationContext context) {
        return context.timeUntilStart().compareTo(LATE_WINDOW) < 0
                ? base.adjust(base.amount().multiply(KEEP_RATE), "Hủy trước giờ khám dưới 24 giờ: trừ phí 30%")
                : base;
    }
}
