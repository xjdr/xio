package com.xjeffrose.xio.http;

import com.xjeffrose.xio.core.internal.UnstableApi;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;

@UnstableApi
public interface Request extends Traceable {

  boolean startOfStream();

  // TODO(CK): move this here from StreamingData?
  // boolean endOfStream();

  HttpMethod method();

  String path();

  String version();

  Headers headers();

  default String host() {
    return headers().get(HttpHeaderNames.HOST.toString());
  }

  default String host(String defaultValue) {
    String result = host();
    if (result == null || result.isEmpty()) {
      return defaultValue;
    }
    return result;
  }

  int streamId();

  default boolean hasBody() {
    return false;
  }

  default ByteBuf body() {
    return Unpooled.EMPTY_BUFFER;
  }

  boolean keepAlive();

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
