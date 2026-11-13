package com.medbooking.patterns.cancellation;

import com.medbooking.dto.response.CancellationPreviewResponse;
import com.medbooking.entity.Appointment;
import com.medbooking.entity.Payment;
import com.medbooking.patterns.cancellation.chain.*;
import com.medbooking.patterns.cancellation.refund.*;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class CancellationAdvisor {
    private final CancellationRuleHandler ruleChain;
    private final RefundCalculator refundCalculator;

    public CancellationAdvisor() {
        ruleChain = new StatusRuleHandler();
        ruleChain.linkWith(new PastAppointmentRuleHandler()).linkWith(new MinimumNoticeRuleHandler());
        refundCalculator = new CardProcessingFeeDecorator(
                new LateCancellationFeeDecorator(new BaseRefundCalculator()));
    }

    public CancellationPreviewResponse advise(Appointment appointment, Payment payment, LocalDateTime now) {
        CancellationContext context = new CancellationContext(appointment, payment, now);
        CancellationDecision decision = ruleChain.handle(context);
        RefundQuote quote = decision.allowed() ? refundCalculator.quote(context)
                : new RefundQuote(BigDecimal.ZERO, List.of());
        return new CancellationPreviewResponse(appointment.getId(), appointment.getStatus().name(),
                decision.allowed(), decision.reason(), context.startAt(), now,
                context.isPaid() ? payment.getAmount() : BigDecimal.ZERO, quote.amount(), quote.notes());
    }
}
