package com.xjeffrose.xio;

import java.nio.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;

import com.xjeffrose.log.*;

//TODO: Need to actually handle setting and getting the body of the response

class HttpResponse {
  private static final Logger log = Log.getLogger(HttpResponse.class.getName());

  private ResponseBuilder resp;

  HttpResponse() {
    this.resp = defaultNotFound();
  }

  public ByteBuffer[] get() {
    return resp.build();
  }

  public void ok() {
    resp = defaultResponse();
  }

  public void notFound() {
    resp = defaultNotFound();
  }

  public void body(String body) {
    resp = resp.addHeader("Content-Length", Integer.toString(body.length()));
    resp = resp.addBody(body);
  }

  static class ResponseBuilder {
    private String protocol;
    private String responseCode;
    private String responseString;
    //TODO: Needed to add this or else I kept getting a ConcurrentModification
    //Exception. FIXME
    private List<String> headers = new CopyOnWriteArrayList<String>();
    private String body = new String();;

    static public ResponseBuilder newBuilder() {
      return new ResponseBuilder();
    }

    public ResponseBuilder protocol(String protocol) {
      this.protocol = protocol;
      return this;
    }

    public ResponseBuilder responseCode(String code) {
      this.responseCode = code;
      return this;
    }

    public ResponseBuilder responseString(String rString) {
      this.responseString = rString;
      return this;
    }

    public ResponseBuilder addHeader(String name, String value) {
      headers.add(name + ": " + value);
      return this;
    }

    public ResponseBuilder addBody(String body) {
      this.body += body;
      return this;
    }

    public ByteBuffer[] build() {
      ByteBuffer[] buffers = {
        // Response Line
        ByteBuffer.wrap(protocol.getBytes()),
        ByteBuffer.wrap(responseCode.getBytes()),
        ByteBuffer.wrap(responseString.getBytes()),
        ByteBuffer.wrap(new String("\r\n").getBytes()),
        // headers
        _headers(),
        ByteBuffer.wrap(new String("\r\n").getBytes()),
        //body
        _body(),
      };

      return buffers;
    }

    private ByteBuffer _headers() {
      int sum = 0;
      for (String header : headers) {
        sum += header.length();
        sum += 2; // \r\n
      }
      //TODO: Needed to add the +128 to account for Content-Length header. Am
      //prob doing something wrong. FIXME
      final ByteBuffer buffer = ByteBuffer.allocateDirect(sum + 128);
      for (String header : headers) {
        buffer.put(ByteBuffer.wrap(header.getBytes()));
        buffer.put(ByteBuffer.wrap(new String("\r\n").getBytes()));
      }

      buffer.flip();
      return buffer;
    }

    private ByteBuffer _body() {
      return ByteBuffer.wrap(body.getBytes());
    }
  }

  public ResponseBuilder defaultResponse() {
    return ResponseBuilder.newBuilder()
                         .protocol("HTTP/1.1 ")
                         .responseCode("200 ")
                         .responseString("OK")
                         .addHeader("Date", ZonedDateTime
                                    .now(ZoneId.of("UTC"))
                                    .format(DateTimeFormatter.RFC_1123_DATE_TIME))
                         .addHeader("Content-Type", "text/html")
                         .addHeader("Server", "xio");
  }

  public ResponseBuilder defaultNotFound() {
    return ResponseBuilder.newBuilder()
                         .protocol("HTTP/1.1 ")
                         .responseCode("404 ")
                         .responseString("Not Found")
                         .addHeader("Date", ZonedDateTime
                                    .now(ZoneId.of("UTC"))
                                    .format(DateTimeFormatter.RFC_1123_DATE_TIME))
                         .addHeader("Content-Length", "0")
                         .addHeader("Content-Type", "text/html")
                         .addHeader("Server", "xio");
  }
}
