package com.medbooking.patterns.cancellation.refund;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Số tiền hoàn dự kiến kèm danh sách diễn giải từng bước tính. */
public record RefundQuote(BigDecimal amount, List<String> notes) {

    public static RefundQuote of(BigDecimal amount, String note) {
        List<String> notes = new ArrayList<>();
        notes.add(note);
        return new RefundQuote(amount, notes);
    }

    /** Tạo báo giá mới với số tiền đã điều chỉnh và thêm một dòng diễn giải. */
    public RefundQuote adjust(BigDecimal newAmount, String note) {
        List<String> merged = new ArrayList<>(notes);
        merged.add(note);
        return new RefundQuote(newAmount, merged);
    }
}
