package com.xjeffrose.xio.backend.server;

import static spark.Spark.*;

import com.codahale.metrics.health.HealthCheck;
import com.google.gson.Gson;
import com.nordstrom.xrpc.server.Server;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import spark.Request;
import spark.Response;
import spark.resource.ClassPathResource;
import spark.resource.Resource;

@Slf4j
public class Main {
  public static void main(String args[]) throws Exception {
    if (args.length < 3) {
      throw new RuntimeException(
          "please specify arguments for server: \n"
              + "port number \n"
              + "header-tag, \n"
              + "h2 (true|false) \n"
              + "h1 tls (true|false) - optional and ignored for h2, default true");
    }

    // header-tag might be the ip address of this host or any other information you
    // would like to use to identify the traffic served up by this host
    final int port = Integer.parseInt(args[0]);
    final String name = args[1];
    final Boolean h2Capable = Boolean.parseBoolean(args[2]);

    if (h2Capable) {
      Config config = ConfigFactory.load("application.conf");
      Server server = new Server(config.getConfig("xrpc"), port);
      configureXrpc(server, name);

      try {
        server.listenAndServe();
      } catch (IOException e) {
        log.error("Failed to start people server", e);
      }

      Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown));
    } else {
      final Boolean tls;
      if (args.length > 3) {
        tls = Boolean.parseBoolean(args[3]);
      } else {
        tls = true;
      }
      setupSpark(port, name, tls);
      awaitInitialization();
      System.out.println("Active Connections");
    }
  }

  private static void setupSpark(int port, String name, boolean tls) throws Exception {
    if (tls) {
      Resource keystore = new ClassPathResource("snakeoil.jks");
      URL url = keystore.getURL();
      String path = url.toString();
      secure(path, "snakeoil", path, "snakeoil");
    }
    port(port);
    get("/", (req, res) -> setupSparkResponse(name, req, res));
    post("/", (req, res) -> setupSparkResponse(name, req, res));
  }

  private static Object setupSparkResponse(String name, Request req, Response res) {
    res.type("application/json");
    Optional<String> echoValue = Optional.ofNullable(req.headers("x-echo"));
    Optional<String> optionalMethod = Optional.ofNullable(req.requestMethod());
    String methodValue = "";
    if (optionalMethod.isPresent()) {
      methodValue = optionalMethod.get();
    }
    Optional<String> tagValue = Optional.ofNullable(name);
    res.header("x-echo", echoValue.orElse("none"));
    res.header("x-method", methodValue);
    res.header("x-tag", tagValue.orElse("no tag"));
    Kraken kraken = new Kraken("Release", "the Kraken");
    return new Gson().toJson(kraken);
  }

  private static void configureXrpc(Server server, String name) {
    // Add handlers for routes
    server.addRoutes(new XrpcKrakenRoutes(name));

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
}
