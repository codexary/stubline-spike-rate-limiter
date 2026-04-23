[English](README.md) | 简体中文

# Token bucket 限流器

Token bucket 允许容量以内的突发请求，然后以固定速率补充 token。这个 spike 对比故意不安全的 check-then-act 实现、`synchronized` 实现和 lock-free 实现。后者把 token 数和补充时间放进一个不可变 record，以 `AtomicReference` 保存；CAS loop 在其他线程先修改状态时重试。可注入的单调时钟使补充测试精确，并验证长时间空闲不会超出容量或溢出。

安装 Java 21。在仓库根目录运行：

```sh
./mvnw -q verify
./mvnw -q exec:java -Dexec.mainClass=com.elsworne.stubline.spike.ratelimit.ConcurrencyHarness
./mvnw -q exec:java -Dexec.mainClass=com.elsworne.stubline.spike.ratelimit.RateLimitDemo
```

第一个演示使用冻结时钟同时启动 32 个线程。在最多 100 轮内，它会输出超过容量的 unsafe 通过数，然后两个安全实现各恰好通过五个。 本地 HTTP 演示以 `X-Client-Id: demo` 发送 12 个请求；预期输出为 `200=5 429=7`。filter 对受限请求返回 `429` 和 `Retry-After: 1`，并按 client ID 分别维护 bucket。

生产环境通常使用 Bucket4j、Resilience4j RateLimiter 或 API gateway 限流，获得经过检验的策略和运维控制。负载均衡器后单个进程的 bucket 不够：每个副本有自己的计数器，整体限制会随副本数增加。该 spike 为开售期间保护 order endpoint 提供依据；整个集群共享的配额需要共享存储或 gateway。

## 许可证

本仓库采用 [MIT License](LICENSE)。

## 关于本仓库

本仓库包含一项针对 Stubline 的独立实验。README 说明运行命令与观察结果。

本仓库的代码与文档借助 AI 工具编写；记录的输出都来自实际运行。
