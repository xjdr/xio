package com.xjeffrose.xio.client;

import io.netty.channel.CombinedChannelDuplexHandler;

public class RequestMuxerCodec extends CombinedChannelDuplexHandler<RequestMuxerDecoder, RequestMuxerEncoder> {

  public RequestMuxerCodec() {
    super(new RequestMuxerDecoder(), new RequestMuxerEncoder());
  }

}
