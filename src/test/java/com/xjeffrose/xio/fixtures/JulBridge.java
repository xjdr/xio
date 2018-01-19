package com.xjeffrose.xio.fixtures;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.mockwebserver.SocketPolicy;
import com.xjeffrose.xio.fixtures.OkHttpUnsafe;
import com.xjeffrose.xio.application.Application;
import com.xjeffrose.xio.bootstrap.ApplicationBootstrap;
import com.xjeffrose.xio.pipeline.SmartHttpPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TestName;
import com.xjeffrose.xio.application.ApplicationConfig;
import io.netty.channel.ChannelHandler;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.ConfigFactory;
import com.xjeffrose.xio.SSL.TlsConfig;
import com.typesafe.config.Config;
import okhttp3.RequestBody;
import okhttp3.MediaType;
import okhttp3.Protocol;
import java.util.Arrays;
import java.util.List;
import com.xjeffrose.xio.client.ClientConfig;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.junit.BeforeClass;

public abstract class JulBridge {
  private static int dummy = -1;

  public static synchronized void initialize() {
    if (dummy == -1) {
      SLF4JBridgeHandler.removeHandlersForRootLogger();
      SLF4JBridgeHandler.install();
      dummy = 0;
    }
  }
}
