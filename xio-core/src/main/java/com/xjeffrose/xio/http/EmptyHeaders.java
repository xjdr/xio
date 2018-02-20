package com.xjeffrose.xio.http;

import com.xjeffrose.xio.core.internal.UnstableApi;

// TODO(CK): optimize emptiness with get calls that return null;
@UnstableApi
public class EmptyHeaders extends DefaultHeaders {

  public static final EmptyHeaders INSTANCE = new EmptyHeaders();

  private EmptyHeaders() {
    super();
  }
}
