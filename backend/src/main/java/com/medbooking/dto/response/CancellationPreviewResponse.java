package com.medbooking.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** A read-only estimate; no payment gateway refund is executed. */
public record CancellationPreviewResponse(
        Integer appointmentId, String status, boolean allowed, String reason,
        LocalDateTime startAt, LocalDateTime evaluatedAt,
        BigDecimal paidAmount, BigDecimal refundAmount, List<String> refundNotes) {
    public CancellationPreviewResponse { refundNotes = List.copyOf(refundNotes); }

    // Compatibility with the original cancellation-preview API.
    @JsonProperty("cancellable")
    public boolean cancellable() { return allowed; }

    @JsonProperty("rejectReason")
    public String rejectReason() { return reason; }
}
