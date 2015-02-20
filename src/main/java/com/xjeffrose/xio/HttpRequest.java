package com.xjeffrose.xio;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;

import com.xjeffrose.log.*;

class HttpRequest extends HttpObject {
  private static final Logger log = Log.getLogger(HttpRequest.class.getName());

  HttpRequest() {
  }

  static class RequestBuilder {
    static public RequestBuilder newBuilder() {
      return new RequestBuilder();
    }

    private String method;
    private String path;
    private String protocol;
    //TODO: Fix this. Should not need to be Concurrent Class
    private List<String> headers = new CopyOnWriteArrayList<String>();

    public RequestBuilder method(String method) {
      this.method = method;
      return this;
    }

    public RequestBuilder path(String path) {
      this.path = path;
      return this;
    }

    public RequestBuilder protocol(String protocol) {
      this.protocol = protocol;
      return this;
    }

    public RequestBuilder addHeader(String name, String value) {
      headers.add(name + ": " + value);
      return this;
    }

    public ByteBuffer[] build() {
      ByteBuffer[] buffers = {
        // Request Line
        ByteBuffer.wrap(method.getBytes()),
        ByteBuffer.wrap(path.getBytes()),
        ByteBuffer.wrap(protocol.getBytes()),
        ByteBuffer.wrap(new String("\r\n").getBytes()),
        // headers
        _headers(),
        ByteBuffer.wrap(new String("\r\n\r\n").getBytes()),
      };

      return buffers;
    }

    private ByteBuffer _headers() {
      int sum = 0;
      for (String header : headers) {
        sum += header.length();
        sum += 2; // \r\n
      }

      ByteBuffer buffer = ByteBuffer.allocateDirect(sum);
      for (String header : headers) {
        buffer.put(ByteBuffer.wrap(header.getBytes()));
        buffer.put(ByteBuffer.wrap(new String("\r\n").getBytes()));
      }

      buffer.flip();
      return buffer;
    }
  }

  static public ByteBuffer[] defaultRequest() {
    return RequestBuilder.newBuilder()
                         .method("GET")
                         .path("/")
                         .protocol("HTTP/1.1")
                         .addHeader("User-Agent", "xio/0.1")
                         .addHeader("Host", "localhost:8000")
                         .addHeader("Accept", "*/*")
                         .build();
  }

}
