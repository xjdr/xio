package com.xjeffrose.xio.client;


import com.google.common.net.HostAndPort;
import com.xjeffrose.xio.core.XioSecurityHandlers;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpClientCodec;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public class HttpClientConnector extends AbstractClientConnector<HttpClientChannel> {
  private final URI endpointUri;

  public HttpClientConnector(String hostNameAndPort, String servicePath)
      throws URISyntaxException {
    this(hostNameAndPort, servicePath, defaultProtocolFactory());
  }

  public HttpClientConnector(URI uri) {
    this(uri, defaultProtocolFactory());
  }

  public HttpClientConnector(String hostNameAndPort, String servicePath, XioProtocolFactory protocolFactory)
      throws URISyntaxException {
    super(new InetSocketAddress(HostAndPort.fromString(hostNameAndPort).getHostText(),
            HostAndPort.fromString(hostNameAndPort).getPortOrDefault(80)),
        protocolFactory);

    this.endpointUri = new URI("http", hostNameAndPort, servicePath, null, null);
  }

  public HttpClientConnector(URI uri, XioProtocolFactory protocolFactory) {
    super(toSocketAddress(HostAndPort.fromParts(checkNotNull(uri).getHost(), getPortFromURI(uri))),
        protocolFactory);

    checkArgument(uri.isAbsolute() && !uri.isOpaque(),
        "HttpClientConnector requires an absolute URI with a path");

    this.endpointUri = uri;
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
            endpointUri.getHost(),
            endpointUri.getPath());
    channel.getNettyChannel().getPipeline().addLast("Xio Client Handler", channel);
    return channel;
  }

  @Override
  public ChannelPipelineFactory newChannelPipelineFactory(final int maxFrameSize, XioClientConfig clientConfig) {
    return new ChannelPipelineFactory() {
      @Override
      public ChannelPipeline getPipeline()
          throws Exception {
        ChannelPipeline cp = Channels.pipeline();
        XioSecurityHandlers securityHandlers = clientConfig.getSecurityFactory().getSecurityHandlers(clientConfig);
        cp.addLast("encryptionHandler", securityHandlers.getEncryptionHandler());
        cp.addLast("httpClientCodec", new HttpClientCodec());
        cp.addLast("chunkAggregator", new HttpChunkAggregator(maxFrameSize));
        return cp;
      }
    };
  }

  @Override
  public String toString() {
    return endpointUri.toString();
  }

}
