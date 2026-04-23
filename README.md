English | [简体中文](README.zh-CN.md)

# Token-bucket rate limiter

A token bucket permits a burst up to its capacity, then refills at a fixed rate. This spike compares a deliberately unsafe check-then-act implementation, a `synchronized` implementation and a lock-free implementation. The latter stores tokens and refill time together in one immutable record inside an `AtomicReference`; a CAS loop retries when another thread changed that state first. An injectable monotonic clock makes refill tests exact and demonstrates that long idle periods cannot exceed capacity or overflow.

Install Java 21. From the repository root:

```sh
./mvnw -q verify
./mvnw -q exec:java -Dexec.mainClass=com.elsworne.stubline.spike.ratelimit.ConcurrencyHarness
./mvnw -q exec:java -Dexec.mainClass=com.elsworne.stubline.spike.ratelimit.RateLimitDemo
```

The first demo starts 32 threads together against a frozen clock. It prints an unsafe admission count above capacity within at most 100 rounds, followed by exactly five admissions for each safe implementation. The local HTTP demo sends 12 requests with `X-Client-Id: demo`; expected output is `200=5 429=7`. Its filter answers a limited request with `429` and `Retry-After: 1`, and maintains separate buckets per client ID.

Production systems commonly use Bucket4j, Resilience4j RateLimiter or API gateway limits for tested policies and operational controls. A bucket in one process is insufficient behind a load balancer: each replica has its own counters, so the aggregate limit grows with replica count. This spike informs protection of the order endpoint during an on-sale; a shared store or gateway is needed for a fleet-wide quota.

## License

This repository is licensed under the [MIT License](LICENSE).

## About this repository

This repository contains a focused Stubline experiment. The README documents its commands and observed results.

The code and documents in this repository were written with the help of AI tools; every recorded output comes from actually running them.

## Findings

The experiment results and their effect on the backend decision are recorded in [FINDINGS.md](FINDINGS.md).
