package com.xjeffrose.xio.client;


import com.xjeffrose.xio.core.XioException;
import com.xjeffrose.xio.core.XioTransportException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.util.Timer;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import org.apache.log4j.Logger;


@NotThreadSafe
public class TcpClientChannel extends AbstractClientChannel {
  private static final Logger log = Logger.getLogger(TcpClientChannel.class.getName());

  private final Channel underlyingNettyChannel;
  private final Timer timer;
  private final XioProtocolFactory protocolFactory;

  protected TcpClientChannel(Channel channel,
                             Timer timer,
                             XioProtocolFactory protocolFactory) {
    super(channel, timer, protocolFactory);

    this.underlyingNettyChannel = channel;
    this.timer = timer;
    this.protocolFactory = protocolFactory;
  }

  @Override
  protected ByteBuf extractResponse(Object message) throws XioTransportException, XioException {
    if (message == null) {
      throw new XioTransportException("Response was null");
    }
    return ((ByteBuf) message).retain();
  }

  @Override
  protected ChannelFuture writeRequest(@Nullable ByteBuf request) {
    return underlyingNettyChannel.writeAndFlush(request);
  }
}