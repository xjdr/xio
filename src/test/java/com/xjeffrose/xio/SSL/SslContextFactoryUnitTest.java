package com.xjeffrose.xio.SSL;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.DefaultEventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.local.LocalServerChannel;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

import static io.netty.handler.codec.http2.Http2CodecUtil.isStreamIdValid;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.xjeffrose.xio.http.Recipes;
import com.xjeffrose.xio.SSL.SelfSignedX509CertGenerator;
import com.xjeffrose.xio.SSL.X509Certificate;

import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.ssl.OpenSsl;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.channel.embedded.EmbeddedChannel;

import com.typesafe.config.ConfigFactory;

import java.util.Arrays;

import static io.netty.handler.codec.http.HttpMethod.*;
import static io.netty.handler.codec.http.HttpVersion.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;

import org.hamcrest.core.IsCollectionContaining;
import org.junit.Assert;
import org.junit.Test;

public class SslContextFactoryUnitTest extends Assert {


  @Test
  public void buildServerContext() throws Exception {
    TlsConfig tlsConfig = new TlsConfig(ConfigFactory.load().getConfig("xio.testServer.settings.tls"));
    SslContext context = SslContextFactory.buildServerContext(tlsConfig);
  }


  @Test
  public void buildClientContext() throws Exception {
    TlsConfig tlsConfig = new TlsConfig(ConfigFactory.load().getConfig("xio.testServer.settings.tls"));
    SslContext context = SslContextFactory.buildClientContext(tlsConfig);
  }
}
