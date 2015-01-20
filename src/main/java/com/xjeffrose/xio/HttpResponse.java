package com.xjeffrose.xio;

import java.nio.*;
import java.util.*;
import java.util.logging.*;

import com.xjeffrose.log.*;

class HttpResponse {
  private static final Logger log = Log.getLogger(HttpResponse.class.getName());

  private final ChannelBuffer cb = new ChannelBuffer();
  private final HttpVersion httpVersion = new HttpVersion();
  private final HttpStatus httpStatus = new HttpStatus();
  private final HttpStatusCode httpStatusCode = new HttpStatusCode();
  private final Headers headers = new Headers();
  private final Body body = new Body();

  public String httpVersion() {
    return ChannelBuffer.toString(httpVersion.get());
  }

  public String statusCode() {
    return ChannelBuffer.toString(httpStatusCode.get());
  }

  public String status() {
    return ChannelBuffer.toString(httpStatus.get());
  }

  public ByteBuffer defaultResponse() {
    httpVersion.set("HTTP/1.1 ");
    httpStatusCode.set("200 ");
    httpStatus.set("OK");
    headers.set("Content-Length", "40"); // change to actual length of the body
    headers.set("Content-Type", "text/html"); // default text/html for now
    body.set("<html><body>HELLO WORLD!</body></html>");

    return get();
  }

  private ByteBuffer get() {
    cb.put(httpVersion.get());
    cb.put(httpStatusCode.get());
    cb.put(httpStatus.get());
    cb.put(headers.get());
    cb.put(ByteBuffer.wrap(new String("\r\n\r\n").getBytes()));
    cb.put(body.get());

    return cb.get();
  }


  class HttpVersion {
    private ByteBuffer httpVersion;

    HttpVersion() {
    }

    public void set(String version) {
      httpVersion = ByteBuffer.wrap(new String(version).getBytes());
    }

    public ByteBuffer get() {
      return httpVersion;
    }
  }

  class HttpStatusCode {
    private ByteBuffer httpStatusCode;

    HttpStatusCode() {
    }

    public void set(String statusCode) {
      httpStatusCode = ByteBuffer.wrap(new String(statusCode).getBytes());
    }

    public ByteBuffer get() {
      return httpStatusCode;
    }
  }

  class HttpStatus {
    private ByteBuffer httpStatus;

    HttpStatus() {
    }

    public void set(String status) {
      httpStatus = ByteBuffer.wrap(new String(status + "\r\n").getBytes());
    }

    public ByteBuffer get() {
      return httpStatus;
    }
  }

  class Header {
    private final String name;
    private final String value;
    private final ByteBuffer header;

    Header(String name, String value) {
      this.name = name;
      this.value = value;

      header = ByteBuffer.wrap(new String(name + ": " + value + "\r\n").getBytes());
    }

    public ByteBuffer get() {
      return header;
    }
  }

  class Headers {
    private final ByteBuffer headers = ByteBuffer.allocateDirect(256);

    Headers() {
    }

    public void set(String name, String value) {
      final Header h = new Header(name,value);
      headers.put(h.get());
    }

    public ByteBuffer get() {
      headers.flip();
      return headers;
    }

    /* public String get(String name) { */
    /* } */

  }

  class Body {
    private ByteBuffer body;

    Body() {
    }

    public void set(String responseBody) {
      body = ByteBuffer.wrap(new String(responseBody).getBytes());
    }

    public ByteBuffer get() {
      return body;
    }
  }

}
