package com.xjeffrose.xio.backend.server;

import com.nordstrom.xrpc.encoding.Encoder;
import com.nordstrom.xrpc.server.Handler;
import com.nordstrom.xrpc.server.RouteBuilder;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class XrpcKrakenRoutes extends RouteBuilder {
  XrpcKrakenRoutes(String name) {
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
