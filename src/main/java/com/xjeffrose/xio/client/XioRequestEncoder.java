package com.xjeffrose.xio.client;

import com.xjeffrose.xio.tracing.HttpTracingState;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import java.util.List;

public class XioRequestEncoder extends MessageToMessageEncoder<XioRequest> {

  @Override
  protected void encode(ChannelHandlerContext ctx, XioRequest msg, List<Object> out) {
    if (msg.hasContext()) {
      HttpTracingState.setContext(ctx, msg.getContext());
    }

    out.add(msg.getPayload());
  }
}
