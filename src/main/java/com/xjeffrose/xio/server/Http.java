package com.xjeffrose.xio.server;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.concurrent.Executors;


public class Http {

  private Http() {
  }

  public static ListenableFuture<XioServer> serve(String hostAndPort, XioService xioService) {
    ListeningExecutorService executor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(2));
    return executor.submit(() -> {
        return new XioServer(null, xioService);
    });
  };

}
