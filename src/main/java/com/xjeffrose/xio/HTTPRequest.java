package com.xjeffrose.xio;

import java.util.*;
import java.util.logging.*;

import com.xjeffrose.log.*;

class HttpRequest {
  private static final Logger log = Log.getLogger(HttpRequest.class.getName());

  public int http_version_major = 0;
  public int http_version_minor = 0;
  public Method method = new Method();
  public Uri uri = new Uri();
  class Header {
    public String name;
    public String value;
  }
  private Deque<Header> header_list = new ArrayDeque<Header>();
  public Headers headers = new Headers();

  class Method {
    public void push_back(byte in) {
    }
  }

  class Uri {
    public void push_back(byte in) {
    }
  }

  class Headers {
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

    public void back() {
    }
  }

}
