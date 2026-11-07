package com.medbooking.patterns.cancellation.chain;

import com.medbooking.patterns.cancellation.CancellationContext;

import java.util.Optional;

/**
 * Chain of Responsibility (Behavioral): mỗi handler kiểm tra đúng một quy tắc hủy lịch.
 * Nếu vi phạm thì dừng chuỗi và trả lý do; nếu không thì chuyển cho handler kế tiếp.
 */
public abstract class CancellationRuleHandler {
    private CancellationRuleHandler next;

    /** Nối handler kế tiếp và trả về chính nó (cho phép viết a.linkWith(b).linkWith(c)). */
    public CancellationRuleHandler linkWith(CancellationRuleHandler next) {
        this.next = next;
        return next;
    }

    public final CancellationDecision handle(CancellationContext context) {
        Optional<String> violation = violation(context);
        if (violation.isPresent()) {
            return CancellationDecision.rejected(violation.get());
        }
        return next == null ? CancellationDecision.approved() : next.handle(context);
    }

    /** Trả về lý do vi phạm, hoặc rỗng nếu quy tắc này được thỏa mãn. */
    protected abstract Optional<String> violation(CancellationContext context);
}
