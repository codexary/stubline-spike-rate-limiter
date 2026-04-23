English | [简体中文](FINDINGS.zh-CN.md)

# Findings

## What we observed

With 32 threads released together and a frozen clock, the unsafe check-then-act bucket admits more than its five-token capacity within the recorded harness runs. The synchronized and CAS implementations admit exactly five. The HTTP demo sends 12 requests from one client ID and records `200=5 429=7`, with `Retry-After: 1` on limited requests.

## Backend decision

A per-process bucket can bound local admission only if token state changes atomically. Each replica has its own counters, so a fleet-wide quota needs a shared store or gateway. Rate limiting does not itself guarantee that one seat has only one winner. See [backend ADR 0004 — Protect seat reservations](https://github.com/codexary/stubline-backend/blob/main/docs/adr/0004-protect-seat-reservations.md).
