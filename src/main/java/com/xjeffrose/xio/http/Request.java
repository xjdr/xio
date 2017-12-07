package com.xjeffrose.xio.http;

import io.netty.handler.codec.http.HttpMethod;
import com.xjeffrose.xio.core.internal.UnstableApi;
import io.netty.buffer.ByteBuf;

@UnstableApi
public abstract class Request {

  public abstract HttpMethod method();
  public abstract String path();
  public abstract String version();
  public abstract Headers headers();

  public boolean hasBody() {
    return false;
  }

  public ByteBuf body() {
    return null;
  }

  /*
  boolean hasBody()
  requestPrepare
  requestProcess
  requestFinish

  public interface StreamingRequestHandler {
  void prepare();
  void process();
  void finish();
  }

  public interace FullRequestHandler {
  void process();
  }

public class ResponseBuilder {
  public ResponseBuilder addHeader(CharSequence key, CharSequence value);
  public ResponseBuilder headers(Headers headers);
  public ResponseBuilder status(int status);
  public ResponseBuilder body(Body);
  public Response build();
}

full response
streaming response

  */

}
