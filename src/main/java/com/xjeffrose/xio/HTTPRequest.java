package com.xjeffrose.xio;

import java.util.logging.*;

class HTTPRequest {

  public int http_version_major = 0;
  public int http_version_minor = 0;
  private static boolean headers_empty = true;

  static class method {
    public static void push_back(byte in) {
    }
  }

  static class uri {
    public static void push_back(byte in) {
    }
  }

  static class headers {
    public static boolean empty() {
      return headers_empty; //FIX ME!!!!!!
    }

    public void back() {
    }
  }


}
