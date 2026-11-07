package com.medbooking.patterns.cancellation.chain;

import com.medbooking.patterns.cancellation.CancellationContext;

import java.time.Duration;
import java.util.Optional;

/** Quy tắc 3: phải hủy trước giờ khám tối thiểu một khoảng thời gian (mặc định 2 giờ). */
public class MinimumNoticeRuleHandler extends CancellationRuleHandler {
    private final Duration minimumNotice;

    public MinimumNoticeRuleHandler() {
        this(Duration.ofHours(2));
    }

    public MinimumNoticeRuleHandler(Duration minimumNotice) {
        this.minimumNotice = minimumNotice;
    }

    @Override
    protected Optional<String> violation(CancellationContext context) {
        return context.timeUntilStart().compareTo(minimumNotice) < 0
                ? Optional.of("Chỉ được hủy trước giờ khám tối thiểu " + minimumNotice.toHours() + " giờ.")
                : Optional.empty();
    }
}
