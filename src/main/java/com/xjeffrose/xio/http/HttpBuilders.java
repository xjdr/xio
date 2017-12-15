package com.xjeffrose.xio.http;

import com.xjeffrose.xio.core.internal.UnstableApi;

@UnstableApi
public class HttpBuilders {

  public static DefaultFullRequest.Builder newFullRequest() {
    return DefaultFullRequest.builder();
  }

  public static DefaultStreamingRequest.Builder newStreamingRequest() {
    return DefaultStreamingRequest.builder();
  }

  public static DefaultFullResponse.Builder newFullResponse() {
    return DefaultFullResponse.builder();
  }

  public static DefaultStreamingResponse.Builder newStreamingResponse() {
    return DefaultStreamingResponse.builder();
  }

  public static DefaultStreamingData.Builder newStreamingData() {
    return DefaultStreamingData.builder();
  }

}
