package com.medbooking.patterns.cancellation.refund;

import com.medbooking.patterns.cancellation.CancellationContext;

public interface RefundCalculator {
    RefundQuote quote(CancellationContext context);
}
