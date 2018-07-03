package com.xjeffrose.xio.backend.server;

import com.codahale.metrics.health.HealthCheck;
import com.nordstrom.xrpc.encoding.Encoder;
import com.nordstrom.xrpc.server.Handler;
import com.nordstrom.xrpc.server.RouteBuilder;
import com.nordstrom.xrpc.server.Server;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.beans.ConstructorProperties;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class Main {
  public static void main(String args[]) throws Exception {
    if (args.length < 2) {
      throw new RuntimeException("please specify server 'port' and 'header-tag' arguments");
    }

    // header-tag might be the ip address of this host or any other information you
    // would like to use to identify the traffic served up by this host
    final int port = Integer.parseInt(args[0]);
    final String name = args[1];

    Config config = ConfigFactory.load("application.conf");
    Server server = new Server(config.getConfig("xrpc"), port);
    Main.configure(server, name);

    try {
      // Fire away
      server.listenAndServe();
    } catch (IOException e) {
      log.error("Failed to start people server", e);
    }

    Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown));
  }

  public static void configure(Server server, String name) {
    // Add handlers for routes
    server.addRoutes(new KrakenRoutes(name));

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
    KrakenRoutes(String name) {
      Handler handler = (request) -> {
        Kraken kraken = new Kraken("Release", "the Kraken");
        final Encoder encoder = request.connectionContext().encoders().acceptedEncoder(request.acceptHeader());
        ByteBuf buf = request.byteBuf();
        ByteBuf encoded = encoder.encode(buf, request.acceptCharsetHeader(), kraken);

        Optional<String> echoValue = Optional.ofNullable(request.header("x-echo")).map(CharSequence::toString);
        Optional<HttpMethod> optionalMethod = request.method();
        String methodValue = "";
        if (optionalMethod.isPresent()) {
          methodValue = optionalMethod.get().name();
        }
        Optional<String> tagValue = Optional.ofNullable(name);

        Map<String, String> customHeaders = new HashMap<>();
        customHeaders.put("x-echo", echoValue.orElse("none"));
        customHeaders.put("x-method", methodValue);
        customHeaders.put("x-tag", tagValue.orElse("no tag"));
        return request.createResponse(HttpResponseStatus.OK, encoded, encoder.mediaType(), customHeaders);
      };
      get("/", handler);
      post("/", handler);
    }
  }

  private static class Kraken {
    @Getter String title;
    @Getter String description;

    @ConstructorProperties({"title", "description"})
    Kraken(String title, String description) {
      this.title = title;
      this.description = description;
    }
  }
}
