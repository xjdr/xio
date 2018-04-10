package com.xjeffrose.xio.helpers;

import com.xjeffrose.xio.test.OkHttpUnsafe;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
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

  /**
   * Make a synchronous http request.
   *
   * @param address the request address.
   * @param protocols the protocol(s) to support.
   * @return a response.
   */
  public static Response http(InetSocketAddress address, Protocol... protocols) {
    final List<Protocol> protocolList;
    if (protocols.length == 0) {
      protocolList = Collections.singletonList(Protocol.HTTP_1_1);
    } else {
      protocolList = Arrays.asList(protocols);
    }
    String url = buildUrl("http://", address);
    return request(url, new OkHttpClient.Builder().protocols(protocolList).build());
  }

  /**
   * Make a synchronous https request using an unsafe http client.
   *
   * @param address the request address.
   * @param protocols the protocol(s) to support.
   * @return a response.
   */
  public static Response https(InetSocketAddress address, Protocol... protocols) throws Exception {
    String url = buildUrl("https://", address);
    return request(url, OkHttpUnsafe.getUnsafeClient(protocols));
  }
}
