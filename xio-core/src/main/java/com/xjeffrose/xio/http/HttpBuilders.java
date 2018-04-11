package com.xjeffrose.xio.http;

import com.xjeffrose.xio.core.internal.UnstableApi;

@UnstableApi
public class HttpBuilders {

  public static DefaultFullRequest.Builder newFullRequest() {
    return DefaultFullRequest.builder();
  }

  public static DefaultSegmentedRequest.Builder newSegmentedRequest() {
    return DefaultSegmentedRequest.builder();
  }

  public static DefaultFullResponse.Builder newFullResponse() {
    return DefaultFullResponse.builder();
  }

  public static DefaultSegmentedResponse.Builder newSegmentedResponse() {
    return DefaultSegmentedResponse.builder();
  }

  public static DefaultSegmentedData.Builder newSegmentedData() {
    return DefaultSegmentedData.builder();
  }
}
