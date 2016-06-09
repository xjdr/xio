package com.xjeffrose.xio.core;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.concurrent.Executors;

import com.xjeffrose.xio.server.XioServer;
import com.xjeffrose.xio.server.XioService;
import com.xjeffrose.xio.client.XioClient;

import org.apache.log4j.Logger;

public class Http {
  private static final Logger log = Logger.getLogger(Http.class.getName());

  private Http() {
  }

  public static ListenableFuture<XioServer> server(String hostAndPort, XioService xioService) {
    ListeningExecutorService executor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(2));
    return executor.submit(() -> {
        return new XioServer(null, xioService);
    });
  };

  public static ListenableFuture<XioClient> client(String hostAndPort, XioService xioService) {
    ListeningExecutorService executor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(2));
    return executor.submit(() -> {
        return new XioClient("x", xioService);
      });
  };

}
