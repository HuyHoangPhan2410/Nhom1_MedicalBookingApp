package com.medbooking.patterns;

import com.medbooking.entity.*;
import com.medbooking.patterns.cancellation.CancellationAdvisor;
import com.medbooking.patterns.cancellation.CancellationContext;
import com.medbooking.patterns.cancellation.chain.*;
import com.medbooking.patterns.cancellation.refund.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class CancellationPatternsTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 12, 7, 0);
    private final CancellationAdvisor advisor = new CancellationAdvisor();
    private final RefundCalculator calculator = new CardProcessingFeeDecorator(
            new LateCancellationFeeDecorator(new BaseRefundCalculator()));

    @ParameterizedTest
    @CsvSource({"-1,false", "0,false", "1,false", "7199,false", "7200,true", "7201,true", "86400,true"})
    void minimumNoticeBoundaryInSeconds(long seconds, boolean allowed) {
        CancellationContext context = context(seconds, Payment.PaymentMethod.cash, Payment.Status.success, "500000");
        var preview = advisor.advise(context.appointment(), context.payment(), NOW);
        assertEquals(allowed, preview.allowed());
        if (!allowed) {
            assertNotNull(preview.reason());
            assertEquals(0, preview.refundAmount().signum());
        }
    }

    @Test void statusViolationWinsEvenWhenAppointmentIsInThePast() {
        CancellationContext context = context(-1, Payment.PaymentMethod.card, Payment.Status.success, "500000");
        context.appointment().setStatus(Appointment.Status.completed);
        var preview = advisor.advise(context.appointment(), context.payment(), NOW);
        assertTrue(preview.reason().contains("hoàn thành"));
    }

    @Test void pastViolationWinsBeforeMinimumNotice() {
        CancellationContext context = context(0, Payment.PaymentMethod.cash, Payment.Status.pending, "500000");
        assertTrue(advisor.advise(context.appointment(), context.payment(), NOW).reason().contains("đã bắt đầu"));
    }

    @Test void rejectingHandlerNeverCallsTheRestOfTheChain() {
        AtomicInteger calls = new AtomicInteger();
        CancellationRuleHandler first = new CancellationRuleHandler() {
            @Override protected Optional<String> violation(CancellationContext context) { return Optional.of("blocked"); }
        };
        first.linkWith(new CancellationRuleHandler() {
            @Override protected Optional<String> violation(CancellationContext context) {
                calls.incrementAndGet();
                return Optional.empty();
            }
        });
        assertEquals("blocked", first.handle(context(7200, Payment.PaymentMethod.cash, Payment.Status.pending, "1")).reason());
        assertEquals(0, calls.get());
    }

    @Test void minimumNoticeCanBeExtendedWithoutChangingOtherRules() {
        MinimumNoticeRuleHandler rule = new MinimumNoticeRuleHandler(Duration.ofHours(4));
        assertFalse(rule.handle(context(3 * 3600, Payment.PaymentMethod.cash, Payment.Status.pending, "1")).allowed());
        assertThrows(IllegalArgumentException.class, () -> new MinimumNoticeRuleHandler(Duration.ofSeconds(-1)));
    }

    @ParameterizedTest
    @CsvSource({
        "7200,card,500000,340000,3",
        "86399,card,500000,340000,3",
        "86400,card,500000,490000,2",
        "86401,card,500000,490000,2",
        "7200,cash,500000,350000,2",
        "86400,cash,500000,500000,1",
        "7200,card,10000,0,3",
        "86400,card,5000,0,2",
        "7200,card,0,0,1",
        "7200,cash,101,71,2"
    })
    void decoratorOrderAndFeeBoundaries(long seconds, Payment.PaymentMethod method, String amount,
                                       String expected, int noteCount) {
        RefundQuote quote = calculator.quote(context(seconds, method, Payment.Status.success, amount));
        assertEquals(new BigDecimal(expected), quote.amount());
        assertEquals(noteCount, quote.notes().size());
    }

    @ParameterizedTest
    @CsvSource({"pending", "failed"})
    void unpaidOrFailedPaymentHasNoRefundOrFees(Payment.Status status) {
        RefundQuote quote = calculator.quote(context(7200, Payment.PaymentMethod.card, status, "500000"));
        assertEquals(BigDecimal.ZERO, quote.amount());
        assertEquals(1, quote.notes().size());
    }

    @Test void missingPaymentStillAllowsCancellationWithZeroRefund() {
        CancellationContext context = context(7200, Payment.PaymentMethod.cash, Payment.Status.pending, "1");
        var preview = advisor.advise(context.appointment(), null, NOW);
        assertTrue(preview.allowed());
        assertEquals(BigDecimal.ZERO, preview.refundAmount());
        assertEquals(BigDecimal.ZERO, preview.paidAmount());
    }

    @Test void quoteNotesAreImmutableAndAdjustDoesNotMutateInnerQuote() {
        RefundQuote original = RefundQuote.of(new BigDecimal("10000"), "base");
        RefundQuote adjusted = original.adjust(new BigDecimal("-1"), "fee");
        assertEquals(1, original.notes().size());
        assertEquals(2, adjusted.notes().size());
        assertEquals(BigDecimal.ZERO, adjusted.amount());
        assertThrows(UnsupportedOperationException.class, () -> adjusted.notes().add("changed"));
    }

    private CancellationContext context(long seconds, Payment.PaymentMethod method, Payment.Status status, String amount) {
        LocalDateTime start = NOW.plusSeconds(seconds);
        Schedule schedule = new Schedule();
        schedule.setWorkDate(start.toLocalDate());
        schedule.setStartTime(start.toLocalTime());
        Appointment appointment = new Appointment();
        appointment.setId(10);
        appointment.setSchedule(schedule);
        appointment.setStatus(Appointment.Status.confirmed);
        Payment payment = new Payment();
        payment.setAppointment(appointment);
        payment.setPaymentMethod(method);
        payment.setStatus(status);
        payment.setAmount(new BigDecimal(amount));
        return new CancellationContext(appointment, payment, NOW);
    }
}
