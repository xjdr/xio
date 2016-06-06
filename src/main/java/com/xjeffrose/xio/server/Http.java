package com.xjeffrose.xio.server;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

public class Http {
  private static final Logger log = Logger.getLogger(XioServerTransport.class.getName());

  private Http() {
  }

  public static ListenableFuture<XioServer> server(String hostAndPort, XioService xioService) {
    ListeningExecutorService executor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(2));
    return executor.submit(() -> {
        return new XioServer(null, xioService);
    });
  };

}
