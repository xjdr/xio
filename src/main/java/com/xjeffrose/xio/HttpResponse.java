package com.xjeffrose.xio;

import java.nio.*;
import java.util.*;
import java.util.logging.*;

import com.xjeffrose.log.*;


//TODO: This should be a ByteBuffer[] that gets sent to channel.write();

class HttpResponse {
  private static final Logger log = Log.getLogger(HttpResponse.class.getName());

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

  public ByteBuffer[] get() {
    final ByteBuffer[] resp = {
      httpVersion.get(), httpStatus.get(), httpStatusCode.get(),
      headers.get(),
      ByteBuffer.wrap(new String("\r\n\r\n").getBytes()),
      body.get()
    };
    return resp;
  }

  class HttpVersion {
    private final ByteBuffer httpVersion = ByteBuffer.wrap(new String("HTTP/1.1").getBytes());

    HttpVersion() {
    }

    public ByteBuffer get() {
      return httpVersion;
    }
  }

  class HttpStatusCode {
    private final ByteBuffer httpStatusCode = ByteBuffer.wrap(new String("200").getBytes());

    HttpStatusCode() {
    }

    public ByteBuffer get() {
      return httpStatusCode;
    }
  }

  class HttpStatus {
    private final ByteBuffer httpStatus = ByteBuffer.wrap(new String("OK\r\n").getBytes());

    HttpStatus() {
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
    /* private int headerIndex = 0; */
    /* private final ByteBuffer[] headers = new ByteBuffer[256]; */
    private final ByteBuffer headers = ByteBuffer.allocateDirect(512);

    Headers() {
    }

    public void add(String name, String value) {
      final Header h = new Header(name,value);
      headers.put(h.get());
    }

    public ByteBuffer get() {
      return headers;
    }

    /* public String get(String name) { */
    /* } */

  }

  class Body {
    private final ByteBuffer body = ByteBuffer.wrap(new String("html><body>HELLO WORLD!</body></html>").getBytes());

    Body() {
    }

    public ByteBuffer get() {
      return body;
    }
  }

}
