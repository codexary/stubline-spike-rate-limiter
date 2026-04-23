package com.elsworne.stubline.spike.ratelimit;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

/** Local HTTP demo for per-client token buckets and 429 responses. */
public final class RateLimitDemo {
  private RateLimitDemo() {}

  public static HttpServer startServer() throws Exception {
    var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    var buckets = new ConcurrentHashMap<String, TokenBucket>();
    server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
    var context =
        server.createContext(
            "/orders",
            exchange -> {
              try (exchange) {
                exchange.sendResponseHeaders(200, -1);
              }
            });
    context
        .getFilters()
        .add(
            new Filter() {
              @Override
              public String description() {
                return "Per-client token bucket";
              }

              @Override
              public void doFilter(com.sun.net.httpserver.HttpExchange exchange, Chain chain)
                  throws java.io.IOException {
                String id = exchange.getRequestHeaders().getFirst("X-Client-Id");
                if (id == null || id.isBlank()) {
                  exchange.sendResponseHeaders(400, -1);
                  exchange.close();
                  return;
                }
                var bucket =
                    buckets.computeIfAbsent(
                        id, ignored -> Buckets.lockFree(5, 1, System::nanoTime));
                if (!bucket.tryAcquire()) {
                  exchange.getResponseHeaders().set("Retry-After", "1");
                  exchange.sendResponseHeaders(429, -1);
                  exchange.close();
                  return;
                }
                chain.doFilter(exchange);
              }
            });
    server.start();
    return server;
  }

  public static void main(String[] args) throws Exception {
    var server = startServer();
    try (var client = HttpClient.newHttpClient()) {
      int ok = 0, limited = 0;
      for (int i = 0; i < 12; i++) {
        var request =
            HttpRequest.newBuilder(
                    URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/orders"))
                .header("X-Client-Id", "demo")
                .build();
        int status = client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
        if (status == 200) ok++;
        if (status == 429) limited++;
      }
      System.out.printf("200=%d 429=%d%n", ok, limited);
      server.stop(0);
    } finally {
      server.stop(0);
    }
  }
}
