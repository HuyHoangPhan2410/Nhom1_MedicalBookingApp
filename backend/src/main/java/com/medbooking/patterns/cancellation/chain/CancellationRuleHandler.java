package com.medbooking.patterns.cancellation.chain;

import com.medbooking.patterns.cancellation.CancellationContext;

import java.util.Objects;
import java.util.Optional;

/** Chain of Responsibility: the first violation stops the remaining handlers. */
public abstract class CancellationRuleHandler {
    private CancellationRuleHandler next;

    public CancellationRuleHandler linkWith(CancellationRuleHandler next) {
        this.next = Objects.requireNonNull(next);
        return next;
    }

    public final CancellationDecision handle(CancellationContext context) {
        Optional<String> reason = violation(context);
        if (reason.isPresent()) return CancellationDecision.rejected(reason.get());
        return next == null ? CancellationDecision.approved() : next.handle(context);
    }

    protected abstract Optional<String> violation(CancellationContext context);
}
