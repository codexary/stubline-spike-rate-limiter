package com.elsworne.stubline.spike.ratelimit;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongSupplier;

/** Creates unsafe, synchronized and CAS token buckets with an injectable clock. */
public final class Buckets {
  private Buckets() {}

  public static TokenBucket unsafe(long capacity, long refillPerSecond, LongSupplier clock) {
    var settings = new Settings(capacity, refillPerSecond, clock);
    return new TokenBucket() {
      private volatile BucketState state = settings.initial();

      @Override
      public boolean tryAcquire() {
        BucketState next = state.refill(clock.getAsLong(), capacity, settings.nanosPerToken());
        if (next.tokens() == 0) {
          state = next;
          return false;
        }
        Thread.yield();
        state = next.consume();
        return true;
      }
    };
  }

  public static TokenBucket synchronizedBucket(
      long capacity, long refillPerSecond, LongSupplier clock) {
    var settings = new Settings(capacity, refillPerSecond, clock);
    return new TokenBucket() {
      private BucketState state = settings.initial();

      @Override
      public synchronized boolean tryAcquire() {
        state = state.refill(clock.getAsLong(), capacity, settings.nanosPerToken());
        if (state.tokens() == 0) return false;
        state = state.consume();
        return true;
      }
    };
  }

  public static TokenBucket lockFree(long capacity, long refillPerSecond, LongSupplier clock) {
    var settings = new Settings(capacity, refillPerSecond, clock);
    var state = new AtomicReference<>(settings.initial());
    return () -> {
      while (true) {
        BucketState previous = state.get();
        BucketState refilled =
            previous.refill(clock.getAsLong(), capacity, settings.nanosPerToken());
        boolean admitted = refilled.tokens() > 0;
        BucketState next = admitted ? refilled.consume() : refilled;
        if (state.compareAndSet(previous, next)) return admitted;
      }
    };
  }

  private record Settings(long capacity, long refillPerSecond, LongSupplier clock) {
    Settings {
      if (capacity < 1 || refillPerSecond < 1 || refillPerSecond > 1_000_000_000L)
        throw new IllegalArgumentException("Invalid capacity or refill rate");
    }

    long nanosPerToken() {
      return 1_000_000_000L / refillPerSecond;
    }

    BucketState initial() {
      return new BucketState(capacity, clock.getAsLong());
    }
  }
}
