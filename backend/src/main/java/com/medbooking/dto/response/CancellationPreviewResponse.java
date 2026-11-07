package com.medbooking.dto.response;

import java.math.BigDecimal;
import java.util.List;

/** Kết quả xem trước việc hủy lịch: có được hủy không, lý do từ chối và số tiền hoàn dự kiến. */
public record CancellationPreviewResponse(
        Integer appointmentId,
        String status,
        boolean cancellable,
        String rejectReason,
        BigDecimal refundAmount,
        List<String> refundNotes) {}
