package com.xjeffrose.xio;

import java.nio.*;
import java.nio.charset.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;

import com.xjeffrose.log.*;

//TODO: Need to actually handle setting and getting the body of the response

class HttpResponse {
  private static final Logger log = Log.getLogger(HttpResponse.class.getName());

  public int http_version_major = 0;
  public int http_version_minor = 0;
  public final Headers headers = new Headers();
  private ResponseBuilder resp;

  private ByteBuffer bb;

  class Picker {
    private int position = -1;
    private int limit = 0;

    public void tick(int currentPos) {
      if (position == -1) {
        position = currentPos;
      }
      limit++;
    }

    public String get() {
      final byte[] value = new byte[limit];
      if (position > 0) {
        bb.position(position);
      } else {
        bb.position(0);
      }
      bb.get(value);
      return new String(value, Charset.forName("UTF-8"));
    }
  }

  class Method extends Picker {
    public String getMethod() {
      return get();
    }
  }

  class Uri extends Picker {
    public String getUri() {
      return get();
    }
  }

  class Header {

    private final Picker name = new Picker();
    private final Picker value = new Picker();
    private Picker currentPicker;

    Header() {
      currentPicker = name;
    }

    public String name() {
      return name.get();
    }

    public String value() {
      return value.get();
    }

    public void tick(int currentPos) {
      currentPicker.tick(currentPos);
    }

    public void startValue() {
      currentPicker = value;
    }
  }

  class Headers {
    private final Deque<Header> header_list = new ArrayDeque<Header>();

    // TODO prefer list of Header objects to minimize String objects at parse
    // time.
    public Map<String, Header> headerMap = new HashMap<String, Header>();
    private Header currentHeader = null;

    Headers() {
    }

    public void newHeader() {
      if (currentHeader != null) {
        done();
      }
      currentHeader = new Header();
    }

    public void done() {
      headerMap.put(currentHeader.name(), currentHeader);
    }

    public void newValue() {
      currentHeader.startValue();
    }

    public void tick(int currentPos) {
      currentHeader.tick(currentPos);
    }

    public boolean empty() {
      return header_list.size() == 0;
    }

    public String get(String name) {
      Header header = headerMap.get(name);
      if (header != null) {
        return header.value();
      } else {
        return "";
      }
    }
  }

  HttpResponse() {
    this.resp = defaultNotFound();
  }

  public void bb(ByteBuffer pbb) {
    bb = pbb.duplicate();
    bb.flip();
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

  public String httpVersion() {
    return new String("HTTP" +
                      "/" +
                      http_version_major +
                      "." +
                      http_version_minor);
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
