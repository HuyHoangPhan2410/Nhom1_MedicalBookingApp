package com.medbooking.patterns.cancellation.refund;

import com.medbooking.patterns.cancellation.CancellationContext;
import java.math.BigDecimal;

public final class BaseRefundCalculator implements RefundCalculator {
    @Override public RefundQuote quote(CancellationContext context) {
        return context.isPaid()
                ? RefundQuote.of(context.payment().getAmount(), "Số tiền đã thanh toán là cơ sở tính hoàn tiền")
                : RefundQuote.of(BigDecimal.ZERO, "Chưa thanh toán thành công, không có tiền để hoàn");
    }
}
