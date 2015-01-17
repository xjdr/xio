package com.xjeffrose.xio;

import java.util.*;
import java.util.logging.*;

import com.xjeffrose.log.*;

//TODO: Add query string support
//TODO: Get header by name

class HttpRequest {
  private static final Logger log = Log.getLogger(HttpRequest.class.getName());

  public int http_version_major = 0;
  public int http_version_minor = 0;
  public final Method method = new Method();
  public final Uri uri = new Uri();
  public final Headers headers = new Headers();

  private final String regex = "\\s*\\bnull\\b\\s*";

  public String method() {
    return method.getMethod();
  }

  public String uri() {
    return uri.getUri();
  }

  public String getQueryString() {
    String fullString = uri.getUri();
    String qs = fullString.split("?", 1)[1];
    return qs;
  }

  class Method {
    private int position = 0;
    private int limit =0;
    private String methodString = new String();

    public void push_back(String in) {
      methodString += in;
      methodString = methodString.replaceAll(regex, "");
    }

    public String getMethod() {
      return methodString;
    }
  }

  class Uri {
    private int position = 0;
    private int limit =0;
    private String uriString = new String();

    public void push_back(String in) {
      uriString += in;
      uriString = uriString.replaceAll(regex, "");
    }

    public String getUri() {
      return uriString;
    }
  }

  class Header {
    public String name = new String();
    public String value = new String();
  }

  class Headers {
    private final Deque<Header> header_list = new ArrayDeque<Header>();
    private boolean headers_empty = true;

    public boolean empty() {
      return header_list.size() == 0;
    }

    public void push_back() {
      header_list.addLast(new Header());
    }

    public void push_back_name(String name) {
      header_list.getLast().name += name;
    }

    public void push_back_value(String value) {
      header_list.getLast().value += value;
    }

    public Map<String,String> get(String name) {
      return new HashMap<String,String>();
    }

  }

}
