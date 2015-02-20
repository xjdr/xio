
package com.xjeffrose.xio;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;

import com.xjeffrose.log.*;

class HttpObject {
  private static final Logger log = Log.getLogger(HttpRequest.class.getName());

  public int http_version_major = 0;
  public int http_version_minor = 0;
  public final Uri uri = new Uri();
  public final Method method = new Method();
  public final Headers headers = new Headers();
  public final Body body = new Body();
  public final ByteBuffer buffer = ByteBuffer.allocateDirect(4096); // Set maximum request size here
  private final ByteBuffer bb = buffer.duplicate();

  public HttpMethod method_ = HttpMethod.get;

  HttpObject() {
  }

  public enum HttpMethod {
    get,
    post,
    put,
    delete
  };
  public String method() {
    return method.getMethod();
  }

  public String uri() {
    return uri.getUri().toString();
  }

  public String httpVersion() {
    return new String("HTTP" +
                      "/" +
                      http_version_major +
                      "." +
                      http_version_minor);
  }

  public int contentLength() {
    String limitString = headers.get("Content-Length");
    if (!limitString.equals(null) && !limitString.equals("")) {
      return Integer.parseInt(limitString);
    }
    return 0;
  }

  public void setMethod() {
    String meth = method();
    if (meth.equalsIgnoreCase("get")) {
      method_ = HttpMethod.get;
    } else if (meth.equalsIgnoreCase("post")) {
      method_ = HttpMethod.post;
    } else if (meth.equalsIgnoreCase("put")) {
      method_ = HttpMethod.put;
    } else if (meth.equalsIgnoreCase("delete")) {
      method_ = HttpMethod.delete;
    }
  }

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
    public URI getUri() {
      return URI.create(get());
    }

    public Map<String, String> splitQuery(URI uri) throws UnsupportedEncodingException {
      Map<String, String> query_pairs = new LinkedHashMap<String, String>();
      String query = uri.getQuery();
      String[] pairs = query.split("&");
      for (String pair : pairs) {
        int idx = pair.indexOf("=");
        query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
      }
      return query_pairs;
    }
  }

  class Body {
    private int position = 0;
    private int limit = 0;

    public void set(int lastByte) {
      if (position == 0 && limit == 0) {
        position = lastByte;
        limit = position + contentLength() + 1;
      }
    }

    public ByteBuffer get() {
      final ByteBuffer temp = bb.duplicate();
      temp.position(position);
      temp.limit(limit);
      return temp.slice();
    }

    public String toString() {
      final ByteBuffer temp = get();
      final byte[] value = new byte[temp.remaining()];
      temp.get(value);
      return new String(value, Charset.forName("UTF-8"));
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
}
