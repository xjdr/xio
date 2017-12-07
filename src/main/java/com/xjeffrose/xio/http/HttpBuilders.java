package com.xjeffrose.xio.http;

import com.xjeffrose.xio.core.internal.UnstableApi;

@UnstableApi
public class HttpBuilders {

  public static FullRequest.Builder newFullRequest() {
    return FullRequest.builder();
  }

  public static StreamingRequest.Builder newStreamingRequest() {
    return StreamingRequest.builder();
  }

  public static FullResponse.Builder newFullResponse() {
    return FullResponse.builder();
  }

  public static StreamingResponse.Builder newStreamingResponse() {
    return StreamingResponse.builder();
  }

  public static StreamingData.StreamingDataImpl.Builder newStreamingData() {
    return StreamingData.StreamingDataImpl.builder();
  }

}
