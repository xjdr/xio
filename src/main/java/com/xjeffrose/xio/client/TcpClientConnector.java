package com.xjeffrose.xio.client;


import com.google.common.net.HostAndPort;
import com.xjeffrose.xio.core.ChannelStatistics;
import com.xjeffrose.xio.core.ConnectionContextHandler;
import com.xjeffrose.xio.core.XioExceptionLogger;
import com.xjeffrose.xio.core.XioSecurityHandlers;
import com.xjeffrose.xio.server.IdleDisconnectHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

public class TcpClientConnector extends AbstractClientConnector<TcpClientChannel> {
  private static final int NO_WRITER_IDLE_TIMEOUT = 20;
  private static final int NO_ALL_IDLE_TIMEOUT = 20;

  private final ChannelStatistics channelStatistics = new ChannelStatistics(new DefaultChannelGroup(new NioEventLoopGroup().next()));

  public TcpClientConnector(String host, int port) {
    this(host + ":" + Integer.toString(port), defaultProtocolFactory());
  }

  public TcpClientConnector(InetSocketAddress addr) {
    this(addr.getHostString() + ":" + addr.getPort(), defaultProtocolFactory());
  }

  public TcpClientConnector(String hostNameAndPort, XioProtocolFactory protocolFactory) {
    super(new InetSocketAddress(HostAndPort.fromString(hostNameAndPort).getHostText(),
        HostAndPort.fromString(hostNameAndPort).getPortOrDefault(80)), protocolFactory);
  }

  @Override
  public TcpClientChannel newClientChannel(Channel nettyChannel, XioClientConfig clientConfig) {
    TcpClientChannel channel =
        new TcpClientChannel(nettyChannel,
            clientConfig.getTimer(),
            getProtocolFactory());
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
//        cp.addLast("TcpCodec", )
        cp.addLast("idleTimeoutHandler", new IdleStateHandler(
            20000,
            NO_WRITER_IDLE_TIMEOUT,
            NO_ALL_IDLE_TIMEOUT,
            TimeUnit.MILLISECONDS));
        cp.addLast("idleDisconnectHandler", new IdleDisconnectHandler(
            2000,
            NO_WRITER_IDLE_TIMEOUT,
            NO_ALL_IDLE_TIMEOUT));
        cp.addLast("authHandler", securityHandlers.getAuthenticationHandler());
        cp.addLast("exceptionLogger", new XioExceptionLogger());
      }
    };
  }
}
