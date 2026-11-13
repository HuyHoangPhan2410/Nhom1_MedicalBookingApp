package com.medbooking.patterns.cancellation.chain;

import com.medbooking.patterns.cancellation.CancellationContext;

import java.util.Optional;

public final class PastAppointmentRuleHandler extends CancellationRuleHandler {
    @Override protected Optional<String> violation(CancellationContext context) {
        return !context.startAt().isAfter(context.now())
                ? Optional.of("Ca khám đã bắt đầu hoặc đã qua, không thể hủy") : Optional.empty();
    }
}
