package com.medbooking.patterns.cancellation.refund;

import com.medbooking.patterns.cancellation.CancellationContext;
import java.util.Objects;

public abstract class RefundDecorator implements RefundCalculator {
    private final RefundCalculator inner;

    protected RefundDecorator(RefundCalculator inner) { this.inner = Objects.requireNonNull(inner); }

    @Override public final RefundQuote quote(CancellationContext context) {
        RefundQuote base = inner.quote(context);
        return base.amount().signum() <= 0 ? base : adjust(base, context);
    }

    protected abstract RefundQuote adjust(RefundQuote base, CancellationContext context);
}
