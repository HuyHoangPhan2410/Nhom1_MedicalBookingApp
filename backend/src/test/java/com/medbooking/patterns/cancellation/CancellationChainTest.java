package com.medbooking.patterns.cancellation;

import com.medbooking.entity.Appointment;
import com.medbooking.patterns.cancellation.chain.*;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.medbooking.patterns.cancellation.CancellationTestSupport.NOW;
import static com.medbooking.patterns.cancellation.CancellationTestSupport.appointment;
import static org.junit.jupiter.api.Assertions.*;

class CancellationChainTest {
    private CancellationRuleHandler defaultChain() {
        StatusRuleHandler first = new StatusRuleHandler();
        first.linkWith(new PastAppointmentRuleHandler()).linkWith(new MinimumNoticeRuleHandler());
        return first;
    }

    private CancellationContext context(Appointment.Status status, long hoursFromNow) {
        return new CancellationContext(appointment(status, NOW.plusHours(hoursFromNow)), null, NOW);
    }

    @Test
    void allowsWhenEveryRuleIsSatisfied() {
        assertTrue(defaultChain().handle(context(Appointment.Status.confirmed, 48)).allowed());
    }

    @Test
    void statusRuleRejectsFirstEvenIfTimeIsAlsoWrong() {
        CancellationDecision decision = defaultChain().handle(context(Appointment.Status.cancelled, -5));
        assertFalse(decision.allowed());
        assertEquals("Lịch hẹn đã được hủy trước đó.", decision.reason());
    }

    @Test
    void rejectsAppointmentThatAlreadyStarted() {
        CancellationDecision decision = defaultChain().handle(context(Appointment.Status.pending, -1));
        assertFalse(decision.allowed());
        assertTrue(decision.reason().contains("đã bắt đầu"));
    }

    @Test
    void rejectsWhenLessThanTwoHoursAhead() {
        CancellationDecision decision = defaultChain().handle(context(Appointment.Status.pending, 1));
        assertFalse(decision.allowed());
        assertTrue(decision.reason().contains("2 giờ"));
    }

    @Test
    void exactlyTwoHoursAheadIsStillAllowed() {
        assertTrue(defaultChain().handle(context(Appointment.Status.pending, 2)).allowed());
    }

    @Test
    void ocpNewRuleCanBeAddedWithoutChangingExistingHandlers() {
        List<String> visited = new ArrayList<>();
        CancellationRuleHandler custom = new CancellationRuleHandler() {
            @Override
            protected Optional<String> violation(CancellationContext ctx) {
                visited.add("custom");
                return Optional.of("Quy tắc mới: tạm khóa hủy lịch.");
            }
        };
        StatusRuleHandler first = new StatusRuleHandler();
        first.linkWith(new MinimumNoticeRuleHandler(Duration.ofHours(1))).linkWith(custom);

        CancellationDecision decision = first.handle(context(Appointment.Status.pending, 48));
        assertFalse(decision.allowed());
        assertEquals(List.of("custom"), visited);
    }
}
