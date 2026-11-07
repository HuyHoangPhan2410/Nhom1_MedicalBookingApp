package com.medbooking.patterns.cancellation.chain;

/** Kết quả của chuỗi kiểm tra: được phép hủy hoặc bị từ chối kèm lý do. */
public record CancellationDecision(boolean allowed, String reason) {
    public static CancellationDecision approved() {
        return new CancellationDecision(true, null);
    }

    public static CancellationDecision rejected(String reason) {
        return new CancellationDecision(false, reason);
    }
}
