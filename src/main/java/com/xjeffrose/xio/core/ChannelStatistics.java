package com.xjeffrose.xio.core;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.group.ChannelGroup;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@ChannelHandler.Sharable
public class ChannelStatistics extends ChannelDuplexHandler implements XioMetrics {
  public static final String NAME = ChannelStatistics.class.getSimpleName();
  private final AtomicInteger channelCount = new AtomicInteger(0);
  private final AtomicLong bytesRead = new AtomicLong(0);
  private final AtomicLong bytesWritten = new AtomicLong(0);
  private final ChannelGroup allChannels;

  public ChannelStatistics(ChannelGroup allChannels) {
    this.allChannels = allChannels;
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    channelCount.incrementAndGet();
    allChannels.add(ctx.channel());
    ctx.fireChannelActive();
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    channelCount.decrementAndGet();
    allChannels.remove(ctx.channel());
    ctx.fireChannelInactive();
  }
  //TODO: Properly implement
//    public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e)
//      throws Exception {
//    if (e instanceof ChannelStateEvent) {
//      ChannelStateEvent cse = (ChannelStateEvent) e;
//      switch (cse.getState()) {
//        case OPEN:
//          if (Boolean.TRUE.equals(cse.getValue())) {
//            // connect
//            channelCount.incrementAndGet();
//            allChannels.add(e.getChannel());
//          } else {
//            // disconnect
//            channelCount.decrementAndGet();
//            allChannels.remove(e.getChannel());
//          }
//          break;
//        case BOUND:
//          break;
//      }
//    }
//
//    if (e instanceof UpstreamMessageEvent) {
//      UpstreamMessageEvent ume = (UpstreamMessageEvent) e;
//      if (ume.getMessage() instanceof ChannelBuffer) {
//        ChannelBuffer cb = (ChannelBuffer) ume.getMessage();
//        int readableBytes = cb.readableBytes();
//        //  compute stats here, bytes read from remote
//        bytesRead.getAndAdd(readableBytes);
//      }
//    }
//
//    ctx.sendUpstream(e);
//  }
//
//  public void handleDownstream(ChannelHandlerContext ctx, ChannelEvent e)
//      throws Exception {
//    if (e instanceof DownstreamMessageEvent) {
//      DownstreamMessageEvent dme = (DownstreamMessageEvent) e;
//      if (dme.getMessage() instanceof ChannelBuffer) {
//        ChannelBuffer cb = (ChannelBuffer) dme.getMessage();
//        int readableBytes = cb.readableBytes();
//        // compute stats here, bytes written to remote
//        bytesWritten.getAndAdd(readableBytes);
//      }
//    }
//    ctx.sendDownstream(e);
//  }

  public int getChannelCount() {
    return channelCount.get();
  }

  public long getBytesRead() {
    return bytesRead.get();
  }

  public long getBytesWritten() {
    return bytesWritten.get();
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    ctx.fireChannelRead(msg);
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
    ctx.fireChannelReadComplete();
  }
}
