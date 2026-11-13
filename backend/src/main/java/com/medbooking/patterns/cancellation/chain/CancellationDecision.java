package com.medbooking.patterns.cancellation.chain;

public record CancellationDecision(boolean allowed, String reason) {
    public static CancellationDecision approved() { return new CancellationDecision(true, null); }
    public static CancellationDecision rejected(String reason) { return new CancellationDecision(false, reason); }
}
