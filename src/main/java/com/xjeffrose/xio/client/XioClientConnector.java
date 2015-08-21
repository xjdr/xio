package com.xjeffrose.xio.client;

import com.xjeffrose.xio.core.XioTimer;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpClientCodec;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class XioClientConnector {
  private final URI endpointUri;
  private SocketAddress address;
  private String hostname;
  private int port;

  public XioClientConnector(URI uri) {
    checkArgument(uri.isAbsolute() && !uri.isOpaque(),
        "HttpClientConnector requires an absolute URI with a path");

    this.endpointUri = uri;
    getHostAndPort(endpointUri);
  }

  private int getPortFromURI(URI uri) {
    URI uriNN = checkNotNull(uri);
    if (uri.getScheme().toLowerCase().equals("http")) {
      return uriNN.getPort() == -1 ? 80 : uriNN.getPort();
    } else if (uri.getScheme().toLowerCase().equals("https")) {
      return uriNN.getPort() == -1 ? 443 : uriNN.getPort();
    } else {
      throw new IllegalArgumentException("HttpClientConnector only connects to HTTP/HTTPS " +
          "URIs");
    }
  }

  private void getHostAndPort(URI uri) {
    this.hostname = uri.getHost();
    this.port = uri.getPort() != -1 ? uri.getPort() : getPortFromURI(uri);
  }

  public ChannelPipelineFactory newChannelPipelineFactory(final int maxFrameSize, XioClientConfig clientConfig) {
    return new ChannelPipelineFactory() {
      @Override
      public ChannelPipeline getPipeline()
          throws Exception {
        ChannelPipeline cp = Channels.pipeline();
        cp.addLast("httpClientCodec", new HttpClientCodec());
        cp.addLast("chunkAggregator", new HttpChunkAggregator(maxFrameSize));
        return cp;
      }
    };
  }

  public ChannelFuture connect(ClientBootstrap bootstrap) {
    checkArgument(hostname != null, "Hostname must be set");
    checkArgument(port != -1, "Port must be set");
    return bootstrap.connect(new InetSocketAddress(hostname, port));
  }

  public XioClientChannel newXioClientChannel(Channel channel) {

//    HttpClientChannel channel =
//        new HttpClientChannel(nettyChannel,
//            clientConfig.getTimer(),
//            getProtocolFactory(),
//            endpointUri.getHost(),
//            endpointUri.getPath());
//    channel.getNettyChannel().getPipeline().addLast("thriftHandler", channel);
//    return channel;


    return new XioClientChannelImpl(channel, new XioTimer("Xio-Timer"));
  }

}
