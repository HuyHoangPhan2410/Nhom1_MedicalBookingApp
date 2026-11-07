package com.medbooking.patterns.cancellation;

import com.medbooking.entity.Appointment;
import com.medbooking.entity.Payment;
import com.medbooking.patterns.cancellation.refund.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static com.medbooking.patterns.cancellation.CancellationTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

class RefundDecoratorTest {
    private RefundCalculator fullStack() {
        return new CardProcessingFeeDecorator(new LateCancellationFeeDecorator(new BaseRefundCalculator()));
    }

    private CancellationContext context(Payment payment, long hoursFromNow) {
        return new CancellationContext(
                appointment(Appointment.Status.confirmed, NOW.plusHours(hoursFromNow)), payment, NOW);
    }

    private void assertAmount(String expected, RefundQuote quote) {
        assertEquals(0, new BigDecimal(expected).compareTo(quote.amount()),
                "expected " + expected + " but was " + quote.amount());
    }

    @Test
    void baseRefundsFullAmountWhenPaid() {
        Payment cash = payment(Payment.PaymentMethod.cash, Payment.Status.success, "500000");
        assertAmount("500000", new BaseRefundCalculator().quote(context(cash, 72)));
    }

    @Test
    void baseRefundsNothingWhenNotPaid() {
        Payment pending = payment(Payment.PaymentMethod.card, Payment.Status.pending, "500000");
        assertAmount("0", new BaseRefundCalculator().quote(context(pending, 72)));
        assertAmount("0", new BaseRefundCalculator().quote(context(null, 72)));
    }

    @Test
    void earlyCashCancellationGetsFullRefund() {
        Payment cash = payment(Payment.PaymentMethod.cash, Payment.Status.success, "500000");
        RefundQuote quote = fullStack().quote(context(cash, 72));
        assertAmount("500000", quote);
        assertEquals(1, quote.notes().size());
    }

    @Test
    void lateCashCancellationLosesThirtyPercent() {
        Payment cash = payment(Payment.PaymentMethod.cash, Payment.Status.success, "500000");
        RefundQuote quote = fullStack().quote(context(cash, 10));
        assertAmount("350000", quote);
        assertEquals(2, quote.notes().size());
    }

    @Test
    void earlyCardCancellationPaysGatewayFeeOnly() {
        Payment card = payment(Payment.PaymentMethod.card, Payment.Status.success, "500000");
        assertAmount("490000", fullStack().quote(context(card, 72)));
    }

    @Test
    void lateCardCancellationStacksBothDecorators() {
        Payment card = payment(Payment.PaymentMethod.card, Payment.Status.success, "500000");
        RefundQuote quote = fullStack().quote(context(card, 10));
        assertAmount("340000", quote); // 500000 x 0.7 - 10000
        assertEquals(3, quote.notes().size());
    }

    @Test
    void gatewayFeeNeverMakesRefundNegative() {
        Payment card = payment(Payment.PaymentMethod.card, Payment.Status.success, "5000");
        assertAmount("0", fullStack().quote(context(card, 72)));
    }

    @Test
    void decoratorsSkippedWhenNothingToRefund() {
        Payment pending = payment(Payment.PaymentMethod.card, Payment.Status.pending, "500000");
        RefundQuote quote = fullStack().quote(context(pending, 5));
        assertAmount("0", quote);
        assertEquals(1, quote.notes().size());
    }
}
