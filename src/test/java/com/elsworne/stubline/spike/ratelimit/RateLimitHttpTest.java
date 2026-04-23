package com.elsworne.stubline.spike.ratelimit;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;

class RateLimitHttpTest {
  @Test
  void limitsClientAfterBurst() throws Exception {
    var server = RateLimitDemo.startServer();
    try (var client = HttpClient.newHttpClient()) {
      URI url = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/orders");
      for (int i = 0; i < 5; i++) assertEquals(200, send(client, url, "buyer-a").statusCode());
      var limited = send(client, url, "buyer-a");
      assertEquals(429, limited.statusCode());

    } finally {
      server.stop(0);
    }
  }

  @Test
  void retryAfterIsPositiveSeconds() throws Exception {
    var server = RateLimitDemo.startServer();
    try (var client = HttpClient.newHttpClient()) {
      URI url = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/orders");
      for (int i = 0; i < 5; i++) send(client, url, "buyer-a");
      assertTrue(
          Integer.parseInt(
                  send(client, url, "buyer-a").headers().firstValue("Retry-After").orElseThrow())
              > 0);
    } finally {
      server.stop(0);
    }
  }

  @Test
  void eachClientHasIndependentBucket() throws Exception {
    var server = RateLimitDemo.startServer();
    try (var client = HttpClient.newHttpClient()) {
      URI url = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/orders");
      for (int i = 0; i < 5; i++) send(client, url, "buyer-a");
      assertEquals(200, send(client, url, "buyer-b").statusCode());
    } finally {
      server.stop(0);
    }
  }

  private static HttpResponse<Void> send(HttpClient client, URI url, String id) throws Exception {
    return client.send(
        HttpRequest.newBuilder(url).header("X-Client-Id", id).build(),
        HttpResponse.BodyHandlers.discarding());
  }
}
