package com.medbooking.patterns.cancellation.refund;

import com.medbooking.patterns.cancellation.CancellationContext;

/** Decorator trừu tượng: bọc một RefundCalculator khác và bổ sung một bước điều chỉnh. */
public abstract class RefundDecorator implements RefundCalculator {
    private final RefundCalculator inner;

    protected RefundDecorator(RefundCalculator inner) {
        this.inner = inner;
    }

    @Override
    public final RefundQuote quote(CancellationContext context) {
        RefundQuote base = inner.quote(context);
        if (base.amount().signum() <= 0) {
            return base; // không còn gì để khấu trừ
        }
        return adjust(base, context);
    }

    /** Áp dụng khoản khấu trừ riêng của decorator lên báo giá đã tính từ lớp bên trong. */
    protected abstract RefundQuote adjust(RefundQuote base, CancellationContext context);
}
