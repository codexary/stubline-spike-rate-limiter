package com.elsworne.stubline.spike.ratelimit;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

/** Releases many buyers at once and counts admitted requests. */
public final class ConcurrencyHarness {
  private ConcurrencyHarness() {}

  public static int admitted(TokenBucket bucket, int clients) throws Exception {
    try (var workers = Executors.newFixedThreadPool(clients)) {
      var ready = new CountDownLatch(clients);
      var start = new CountDownLatch(1);
      var futures = new ArrayList<java.util.concurrent.Future<Boolean>>();
      for (int i = 0; i < clients; i++)
        futures.add(
            workers.submit(
                () -> {
                  ready.countDown();
                  start.await();
                  return bucket.tryAcquire();
                }));
      ready.await();
      start.countDown();
      int count = 0;
      for (var future : futures) if (future.get()) count++;
      return count;
    }
  }

  public static void main(String[] args) throws Exception {
    int capacity = 5;
    long frozen = 0;
    boolean observed = false;
    for (int i = 0; i < 100 && !observed; i++) {
      int admitted = admitted(Buckets.unsafe(capacity, 1, () -> frozen), 32);
      if (admitted > capacity) {
        System.out.printf("unsafe admitted=%d capacity=%d round=%d%n", admitted, capacity, i + 1);
        observed = true;
      }
    }
    if (!observed) throw new IllegalStateException("Unsafe race was not observed in 100 rounds");
    System.out.printf(
        "synchronized admitted=%d capacity=%d%n",
        admitted(Buckets.synchronizedBucket(capacity, 1, () -> frozen), 32), capacity);
    System.out.printf(
        "CAS admitted=%d capacity=%d%n",
        admitted(Buckets.lockFree(capacity, 1, () -> frozen), 32), capacity);
  }
}
