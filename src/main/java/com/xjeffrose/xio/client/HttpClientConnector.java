package com.xjeffrose.xio.client;


import com.google.common.net.HostAndPort;
import com.xjeffrose.xio.core.*;
import com.xjeffrose.xio.server.IdleDisconnectHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.timeout.IdleStateHandler;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public class HttpClientConnector extends AbstractClientConnector<HttpClientChannel> {
  private static final int NO_WRITER_IDLE_TIMEOUT = 20000;
  private static final int NO_ALL_IDLE_TIMEOUT = 20000;

  private final URI endpointUri;
  private final ChannelStatistics channelStatistics = new ChannelStatistics(new DefaultChannelGroup(new NioEventLoopGroup().next()));

  public HttpClientConnector(String hostNameAndPort, String servicePath)
      throws URISyntaxException {
    this(hostNameAndPort, servicePath, defaultProtocolFactory());
  }

  public HttpClientConnector(URI uri) {
    this(uri, defaultProtocolFactory());
  }

  public HttpClientConnector(URI uri, XioProtocolFactory protocolFactory) {
    super(toSocketAddress(HostAndPort.fromParts(checkNotNull(uri).getHost(), getPortFromURI(uri))),
        protocolFactory);

    checkArgument(uri.isAbsolute() && !uri.isOpaque(),
        "HttpClientConnector requires an absolute URI with a path");

    this.endpointUri = uri;
  }

  public HttpClientConnector(String hostNameAndPort, String servicePath, XioProtocolFactory protocolFactory)
      throws URISyntaxException {
    super(new InetSocketAddress(HostAndPort.fromString(hostNameAndPort).getHostText(),
            HostAndPort.fromString(hostNameAndPort).getPortOrDefault(80)),
        protocolFactory);

    this.endpointUri = new URI("http", hostNameAndPort, servicePath, null, null);
  }

  private static int getPortFromURI(URI uri) {
    URI uriNN = checkNotNull(uri);
    checkState(uri.getScheme() != null, "The entered URL is not valid");
    if (uri.getScheme().toLowerCase().equals("http")) {
      return uriNN.getPort() == -1 ? 80 : uriNN.getPort();
    } else if (uri.getScheme().toLowerCase().equals("https")) {
      return uriNN.getPort() == -1 ? 443 : uriNN.getPort();
    } else {
      throw new IllegalArgumentException("HttpClientConnector only connects to HTTP/HTTPS " +
          "URIs");
    }
  }

  @Override
  public HttpClientChannel newClientChannel(Channel nettyChannel, XioClientConfig clientConfig) {
    HttpClientChannel channel =
        new HttpClientChannel(nettyChannel,
            clientConfig.getTimer(),
            getProtocolFactory(),
            endpointUri);
    channel.getNettyChannel().pipeline().addLast("Xio Client Handler", channel);
    return channel;
  }

  @Override
  public ChannelInitializer<SocketChannel> newChannelPipelineFactory(final int maxFrameSize, XioClientConfig clientConfig) {

    return new ChannelInitializer<SocketChannel>() {
      @Override
      protected void initChannel(SocketChannel channel) throws Exception {
        ChannelPipeline cp = channel.pipeline();

        TimeoutHandler.addToPipeline(cp);
        XioSecurityHandlers securityHandlers = clientConfig.getSecurityFactory().getSecurityHandlers();
        cp.addLast("encryptionHandler", securityHandlers.getEncryptionHandler());
        cp.addLast("connectionContext", new ConnectionContextHandler());
        cp.addLast(ChannelStatistics.NAME, channelStatistics);
        cp.addLast("httpClientCodec", new HttpClientCodec());
//        cp.addLast("defaltor", new HttpContentDecompressor());
        cp.addLast("chunkAggregator", new HttpObjectAggregator(maxFrameSize));
        cp.addLast("idleDisconnectHandler", new XioIdleDisconnectHandler(
              20000,
              NO_WRITER_IDLE_TIMEOUT,
              NO_ALL_IDLE_TIMEOUT,
              TimeUnit.MILLISECONDS));
        cp.addLast("authHandler", securityHandlers.getAuthenticationHandler());
        cp.addLast("exceptionLogger", new XioExceptionLogger());
      }
    };
  }

  @Override
  public String toString() {
    return endpointUri.toString();
  }

}
