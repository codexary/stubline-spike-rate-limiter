package com.elsworne.stubline.spike.ratelimit;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;
import org.junit.jupiter.api.Test;

class TokenBucketTest {
  @Test
  void controlledClockRefillsExactlyAndCapsBurstWithoutOverflow() {
    for (int kind = 0; kind < 2; kind++) {
      var clock = new AtomicLong();
      TokenBucket bucket =
          kind == 0
              ? Buckets.synchronizedBucket(3, 10, clock::get)
              : Buckets.lockFree(3, 10, clock::get);
      for (int i = 0; i < 3; i++) assertTrue(bucket.tryAcquire());
      assertFalse(bucket.tryAcquire());
      clock.addAndGet(100_000_000L);
      assertTrue(bucket.tryAcquire());
      assertFalse(bucket.tryAcquire());
      clock.set(Long.MAX_VALUE);
      for (int i = 0; i < 3; i++) assertTrue(bucket.tryAcquire());
      assertFalse(bucket.tryAcquire());
    }
  }

  @Test
  void safeImplementationsStayWithinCapacityAndUnsafeRaceIsDemonstrable() throws Exception {
    int capacity = 5;
    LongSupplier frozen = () -> 0;
    assertEquals(
        capacity, ConcurrencyHarness.admitted(Buckets.synchronizedBucket(capacity, 1, frozen), 32));
    assertEquals(capacity, ConcurrencyHarness.admitted(Buckets.lockFree(capacity, 1, frozen), 32));
    boolean observed = false;
    for (int round = 0; round < 100 && !observed; round++)
      observed = ConcurrencyHarness.admitted(Buckets.unsafe(capacity, 1, frozen), 32) > capacity;
    assertTrue(observed, "Unsafe race should appear within the bounded demonstration");
  }
}
