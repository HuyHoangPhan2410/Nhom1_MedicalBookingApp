package com.medbooking.patterns.cancellation.refund;

import com.medbooking.patterns.cancellation.CancellationContext;

/** Component của Decorator: giao diện chung cho bộ tính hoàn tiền gốc và các lớp bọc. */
public interface RefundCalculator {
    RefundQuote quote(CancellationContext context);
}
