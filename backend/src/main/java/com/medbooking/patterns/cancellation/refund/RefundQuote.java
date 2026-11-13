package com.medbooking.patterns.cancellation.refund;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Immutable estimate, not a refund transaction or gateway receipt. */
public record RefundQuote(BigDecimal amount, List<String> notes) {
    public RefundQuote {
        amount = Objects.requireNonNull(amount).max(BigDecimal.ZERO).setScale(0, RoundingMode.HALF_UP);
        notes = List.copyOf(notes);
    }
    public static RefundQuote of(BigDecimal amount, String note) {
        return new RefundQuote(amount, List.of(note));
    }
    public RefundQuote adjust(BigDecimal amount, String note) {
        List<String> updated = new ArrayList<>(notes);
        updated.add(note);
        return new RefundQuote(amount, updated);
    }
}
