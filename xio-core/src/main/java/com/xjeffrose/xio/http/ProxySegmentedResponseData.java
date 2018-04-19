package com.xjeffrose.xio.http;

import io.netty.buffer.ByteBuf;

public class ProxySegmentedResponseData extends ProxyResponse implements SegmentedData {

  private SegmentedResponseData delegate;

  public ProxySegmentedResponseData(SegmentedResponseData delegate, int streamId) {
    super(delegate, streamId);
    this.delegate = delegate;
  }

  // region SegmentedData

  @Override
  public ByteBuf content() {
    return delegate.content();
  }

  @Override
  public Headers trailingHeaders() {
    return delegate.trailingHeaders();
  }

  // endregion
}
