package com.elsworne.stubline.spike.ratelimit;

/** Admits a request when a token is available. */
public interface TokenBucket {
  boolean tryAcquire();
}
