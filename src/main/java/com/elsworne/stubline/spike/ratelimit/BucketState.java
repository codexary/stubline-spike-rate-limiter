package com.elsworne.stubline.spike.ratelimit;

/** Immutable token count and last refill instant in monotonic nanoseconds. */
public record BucketState(long tokens, long lastRefillNanos) {
  public BucketState refill(long now, long capacity, long nanosPerToken) {
    if (now <= lastRefillNanos) return this;
    long elapsed = now - lastRefillNanos;
    if (elapsed < 0) elapsed = Long.MAX_VALUE;
    long added = elapsed / nanosPerToken;
    if (added == 0) return this;
    if (added >= capacity - tokens) return new BucketState(capacity, now);
    return new BucketState(tokens + added, lastRefillNanos + added * nanosPerToken);
  }

  public BucketState consume() {
    return new BucketState(tokens - 1, lastRefillNanos);
  }
}
