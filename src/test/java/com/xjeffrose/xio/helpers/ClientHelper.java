package com.xjeffrose.xio.helpers;

import java.io.IOException;

import com.xjeffrose.xio.bootstrap.XioServerBootstrap;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import java.util.Map;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ClientHelper {

  static public Response request(String url) {
    try {
      OkHttpClient client = new OkHttpClient();
      Request request = new Request.Builder()
        .url(url)
        .build()
      ;
      return client.newCall(request).execute();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
