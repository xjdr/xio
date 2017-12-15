package com.xjeffrose.xio.helpers;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import com.xjeffrose.xio.fixtures.OkHttpUnsafe;
import java.io.IOException;
import java.net.InetSocketAddress;

public class ClientHelper {

  static public Response request(String url, OkHttpClient client) {
    try {
      Request request = new Request.Builder()
        .url(url)
        .build()
      ;
      return client.newCall(request).execute();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  static public Response request(String url) {
    return request(url, new OkHttpClient());
  }

  static String buildUrl(String protocol, InetSocketAddress address) {
    return protocol + address.getHostString() + ":" + address.getPort() + "/";
  }

  static public Response http(InetSocketAddress address) {
    String url = buildUrl("http://", address);
    return request(url, new OkHttpClient());
  }

  static public Response https(InetSocketAddress address) {
    String url = buildUrl("https://", address);
    return request(url, OkHttpUnsafe.getUnsafeClient());
  }
}
