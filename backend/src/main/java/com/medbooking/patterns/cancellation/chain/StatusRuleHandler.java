package com.medbooking.patterns.cancellation.chain;

import com.medbooking.patterns.cancellation.CancellationContext;
import com.medbooking.patterns.cancellation.state.AppointmentLifecycle;

import java.util.Optional;

/** Quy tắc 1: trạng thái lịch hẹn phải cho phép hủy (hỏi State pattern). */
public class StatusRuleHandler extends CancellationRuleHandler {
    @Override
    protected Optional<String> violation(CancellationContext context) {
        AppointmentLifecycle lifecycle = AppointmentLifecycle.of(context.appointment());
        return lifecycle.canCancel() ? Optional.empty() : Optional.of(lifecycle.cancelBlockedReason());
    }
}
