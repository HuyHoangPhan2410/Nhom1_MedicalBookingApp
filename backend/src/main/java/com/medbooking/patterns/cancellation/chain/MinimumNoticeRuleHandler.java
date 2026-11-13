package com.medbooking.patterns.cancellation.chain;

import com.medbooking.patterns.cancellation.CancellationContext;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

public final class MinimumNoticeRuleHandler extends CancellationRuleHandler {
    private final Duration minimumNotice;

    public MinimumNoticeRuleHandler() { this(Duration.ofHours(2)); }
    public MinimumNoticeRuleHandler(Duration minimumNotice) {
        this.minimumNotice = Objects.requireNonNull(minimumNotice);
        if (minimumNotice.isNegative()) throw new IllegalArgumentException("Minimum notice must not be negative");
    }

    @Override protected Optional<String> violation(CancellationContext context) {
        return context.timeUntilStart().compareTo(minimumNotice) < 0
                ? Optional.of("Cần hủy lịch trước giờ khám ít nhất " + minimumNotice.toMinutes() + " phút (" + minimumNotice.toHours() + " giờ)")
                : Optional.empty();
    }
}
