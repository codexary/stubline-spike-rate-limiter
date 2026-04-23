package com.elsworne.stubline.spike.ratelimit;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;
import org.junit.jupiter.api.Test;

class TokenBucketTest {
  @Test
  void synchronizedBucketRefillsAtControlledClock() {
    var clock = new AtomicLong();
    assertRefill(Buckets.synchronizedBucket(3, 10, clock::get), clock);
  }

  @Test
  void lockFreeBucketRefillsAtControlledClock() {
    var clock = new AtomicLong();
    assertRefill(Buckets.lockFree(3, 10, clock::get), clock);
  }

  private static void assertRefill(TokenBucket bucket) {
    for (int i = 0; i < 3; i++) assertTrue(bucket.tryAcquire());
    assertFalse(bucket.tryAcquire());
  }

  private static void assertRefill(TokenBucket bucket, AtomicLong clock) {
    assertRefill(bucket);
    clock.addAndGet(100_000_000L);
    assertTrue(bucket.tryAcquire());
    assertFalse(bucket.tryAcquire());
    clock.set(Long.MAX_VALUE);
    for (int i = 0; i < 3; i++) assertTrue(bucket.tryAcquire());
    assertFalse(bucket.tryAcquire());
  }

  @Test
  void synchronizedBucketNeverOverAdmits() throws Exception {
    LongSupplier frozen = () -> 0;
    assertEquals(5, ConcurrencyHarness.admitted(Buckets.synchronizedBucket(5, 1, frozen), 32));
  }

  @Test
  void lockFreeBucketNeverOverAdmits() throws Exception {
    LongSupplier frozen = () -> 0;
    assertEquals(5, ConcurrencyHarness.admitted(Buckets.lockFree(5, 1, frozen), 32));
  }

  @Test
  void unsafeBucketOverAdmitsWithinBoundedRounds() throws Exception {
    LongSupplier frozen = () -> 0;
    boolean observed = false;
    for (int round = 0; round < 100 && !observed; round++)
      observed = ConcurrencyHarness.admitted(Buckets.unsafe(5, 1, frozen), 32) > 5;
    assertTrue(observed);
  }
}
