[English](FINDINGS.md) | 简体中文

# 实验结论

## 观察

32 个线程在冻结时钟下同时请求时，故意采用先检查再更新的实现会在记录的运行中放行超过五个请求；`synchronized` 和 CAS 实现都恰好放行五个。本地 HTTP 演示用同一客户端 ID 发送 12 次请求，结果为 `200=5 429=7`；被限流的响应带有 `Retry-After: 1`。

## 对后端的影响

进程内令牌桶必须原子更新状态，才能限制本进程的放行数。各副本分别计数，因此整个集群的配额需要共享存储或网关。限流本身不能保证同一座位只卖给一名买家。参见[后端 ADR 0004：Protect seat reservations](https://github.com/codexary/stubline-backend/blob/main/docs/adr/0004-protect-seat-reservations.md)。
