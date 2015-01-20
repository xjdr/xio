package com.xjeffrose.xio;

import java.nio.*;
import java.nio.charset.*;
import java.util.*;
import java.util.logging.*;

import com.xjeffrose.log.*;

class HttpRequest {
  private static final Logger log = Log.getLogger(HttpRequest.class.getName());

  public int http_version_major = 0;
  public int http_version_minor = 0;
  public final Uri uri = new Uri();
  public final Method method = new Method();
  public final Headers headers = new Headers();

  private final String regex = "\\s*\\bnull\\b\\s*";
  private ChannelBuffer cb;
  private ByteBuffer bb;

  HttpRequest() {
  }

  public void cb(ChannelBuffer cb) {
    this.cb = cb;
  }

  public String method() {
    return method.getMethod();
  }

  public String uri() {
    return uri.getUri();
  }

  public String httpVersion() {
    return new String("HTTP" +
                      Integer.toString(http_version_major) +
                      "/" +
                      Integer.toString(http_version_minor));
  }

  public String getQueryString() {
    String fullString = uri.getUri();
    String qs = fullString.split("?", 1)[1];
    return qs;
  }

  class Method {
    private int position = 0;
    private int limit =0;
    private byte[] method;

    public void set() {
      position = cb.position();
    }

    public void tick() {
      limit++;
    }

    public String getMethod() {
      method = cb.get(position, limit);
      return new String(method, Charset.forName("UTF-8"));
    }
  }

  class Uri {
    private int position = 0;
    private int limit =0;
    private byte[] uri;

    public void set() {
      position = cb.position();
    }

    public void tick() {
      limit++;
    }

    public String getUri() {
      uri = cb.get(position, limit);
      return new String(uri, Charset.forName("UTF-8"));
    }
  }

  class Header {
    private int position = 0;
    private int limit = 0;
    private byte[] tempHeader;

    Header(int pos, int limit) {
      this.position = pos;
      this.limit = limit;
    }

    public String getHeader() {
      tempHeader = cb.get(position, limit);
      return new String(tempHeader, Charset.forName("UTF-8"));
    }
  }

  class Headers {
    private int position = 0;
    private int limit = 0;

    Headers() {
    }

    public boolean empty() {
      return true;
    }

    public void set() {
      position = cb.position();
    }

    public void tick() {
      limit++;
    }

    public void newHeader() {
      Header h = new Header(position, limit);
    }

    public String getHeaders() {
      return new String();
    }


  }

  /* class Header { */
  /*   public String name = new String(); */
  /*   public String value = new String(); */
  /* } */

  /* class Headers { */
  /*   private final Deque<Header> header_list = new ArrayDeque<Header>(); */
  /*   private boolean headers_empty = true; */
  /*  */
  /*   public boolean empty() { */
  /*     return header_list.size() == 0; */
  /*   } */
  /*  */
  /*   public void push_back() { */
  /*     header_list.addLast(new Header()); */
  /*   } */
  /*  */
  /*   public void push_back_name(String name) { */
  /*     header_list.getLast().name += name; */
  /*   } */
  /*  */
  /*   public void push_back_value(String value) { */
  /*     header_list.getLast().value += value; */
  /*   } */
  /*  */
  /*   public Map<String,String> get(String name) { */
  /*     return new HashMap<String,String>(); */
  /*   } */
  /*  */
  /* } */

}
