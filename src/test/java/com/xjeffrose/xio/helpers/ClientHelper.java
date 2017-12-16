package com.xjeffrose.xio.helpers;

import com.xjeffrose.xio.fixtures.OkHttpUnsafe;
import java.io.IOException;
import java.net.InetSocketAddress;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ClientHelper {

  public static Response request(String url, OkHttpClient client) {
    try {
      Request request = new Request.Builder().url(url).build();
      return client.newCall(request).execute();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static Response request(String url) {
    return request(url, new OkHttpClient());
  }

  static String buildUrl(String protocol, InetSocketAddress address) {
    return protocol + address.getHostString() + ":" + address.getPort() + "/";
  }

  public static Response http(InetSocketAddress address) {
    String url = buildUrl("http://", address);
    return request(url, new OkHttpClient());
  }

  public static Response https(InetSocketAddress address) {
    String url = buildUrl("https://", address);
    return request(url, OkHttpUnsafe.getUnsafeClient());
  }
}
