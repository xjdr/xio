package com.xjeffrose.xio.backend.server;

import com.codahale.metrics.health.HealthCheck;
import com.google.gson.JsonObject;
import com.nordstrom.xrpc.server.Handler;
import com.nordstrom.xrpc.server.RouteBuilder;
import com.nordstrom.xrpc.server.Server;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class Main {
  public static void main(String args[]) throws Exception {
    Config config = ConfigFactory.load("application.conf");
    Server server = new Server(config.getConfig("xrpc"));
    Main.configure(server);

    try {
      // Fire away
      server.listenAndServe();
    } catch (IOException e) {
      log.error("Failed to start people server", e);
    }

    Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown));
  }

  public static void configure(Server server) {
    // Add handlers for routes
    server.addRoutes(new KrakenRoutes());

    // Add a service specific health check
    server.addHealthCheck(
      "simple",
      new HealthCheck() {
        @Override
        protected Result check() {
          System.out.println("Health Check Ran");
          return Result.healthy();
        }
      });
  }

  private static class KrakenRoutes extends RouteBuilder {
    KrakenRoutes() {
      Handler handler = (request) -> {
        JsonObject object = new JsonObject();
        object.addProperty("title", "Release");
        object.addProperty("description", "the Kraken");
        return request.ok(object.toString());
      };
      get("/", handler);
    }
  }
}
