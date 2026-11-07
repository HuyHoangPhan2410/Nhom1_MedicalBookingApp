package com.medbooking.patterns.cancellation.chain;

import com.medbooking.patterns.cancellation.CancellationContext;

import java.util.Optional;

/** Quy tắc 2: không hủy được ca khám đã bắt đầu hoặc đã qua. */
public class PastAppointmentRuleHandler extends CancellationRuleHandler {
    @Override
    protected Optional<String> violation(CancellationContext context) {
        return context.timeUntilStart().isNegative()
                ? Optional.of("Ca khám đã bắt đầu hoặc đã qua, không thể hủy.")
                : Optional.empty();
    }
}
