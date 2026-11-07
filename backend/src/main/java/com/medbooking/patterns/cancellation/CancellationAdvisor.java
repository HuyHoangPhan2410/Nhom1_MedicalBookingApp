package com.medbooking.patterns.cancellation;

import com.medbooking.dto.response.CancellationPreviewResponse;
import com.medbooking.entity.Appointment;
import com.medbooking.entity.Payment;
import com.medbooking.patterns.cancellation.chain.CancellationDecision;
import com.medbooking.patterns.cancellation.chain.CancellationRuleHandler;
import com.medbooking.patterns.cancellation.chain.MinimumNoticeRuleHandler;
import com.medbooking.patterns.cancellation.chain.PastAppointmentRuleHandler;
import com.medbooking.patterns.cancellation.chain.StatusRuleHandler;
import com.medbooking.patterns.cancellation.refund.BaseRefundCalculator;
import com.medbooking.patterns.cancellation.refund.CardProcessingFeeDecorator;
import com.medbooking.patterns.cancellation.refund.LateCancellationFeeDecorator;
import com.medbooking.patterns.cancellation.refund.RefundCalculator;
import com.medbooking.patterns.cancellation.refund.RefundQuote;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Điểm lắp ráp của ba pattern: Chain of Responsibility quyết định có được hủy hay không,
 * State cung cấp luật theo trạng thái, Decorator tính số tiền hoàn.
 * Lớp thuần logic (không truy cập CSDL), nhận "now" từ bên ngoài nên test được ổn định.
 */
@Component
public class CancellationAdvisor {
    private final CancellationRuleHandler ruleChain;
    private final RefundCalculator refundCalculator;

    public CancellationAdvisor() {
        StatusRuleHandler first = new StatusRuleHandler();
        first.linkWith(new PastAppointmentRuleHandler())
             .linkWith(new MinimumNoticeRuleHandler());
        this.ruleChain = first;
        this.refundCalculator = new CardProcessingFeeDecorator(
                new LateCancellationFeeDecorator(new BaseRefundCalculator()));
    }

    public CancellationPreviewResponse advise(Appointment appointment, Payment payment, LocalDateTime now) {
        CancellationContext context = new CancellationContext(appointment, payment, now);
        CancellationDecision decision = ruleChain.handle(context);

        if (!decision.allowed()) {
            return new CancellationPreviewResponse(appointment.getId(), appointment.getStatus().name(),
                    false, decision.reason(), BigDecimal.ZERO, List.of());
        }
        RefundQuote quote = refundCalculator.quote(context);
        return new CancellationPreviewResponse(appointment.getId(), appointment.getStatus().name(),
                true, null, quote.amount().setScale(2, RoundingMode.HALF_UP), quote.notes());
    }
}
