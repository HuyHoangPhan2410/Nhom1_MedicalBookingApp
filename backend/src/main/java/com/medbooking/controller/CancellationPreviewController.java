package com.medbooking.controller;

import com.medbooking.dto.response.ApiResponse;
import com.medbooking.dto.response.CancellationPreviewResponse;
import com.medbooking.patterns.cancellation.CancellationPreviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Xem trước kết quả hủy lịch (được hủy không, hoàn bao nhiêu) trước khi gọi PUT /cancel. */
@Tag(name = "Appointments", description = "Appointment booking, lookup, cancellation and rescheduling")
@RestController
@RequestMapping("/api/appointments")
public class CancellationPreviewController {
    private final CancellationPreviewService previewService;

    public CancellationPreviewController(CancellationPreviewService previewService) {
        this.previewService = previewService;
    }

    @Operation(summary = "Preview cancellation",
            description = "Checks the cancellation rules and estimates the refund without changing any data.")
    @GetMapping("/{id}/cancellation-preview")
    public ApiResponse<CancellationPreviewResponse> preview(@PathVariable Integer id) {
        return ApiResponse.success(previewService.preview(id));
    }
}
