package com.xjeffrose.xio.http;

/**
 * Interface representing a HTTP1/2 Message (Request/Response) Message: A complete sequence of
 * frames that map to a logical request or response message. See: <a href= ></a>
 */
public interface Message {

  int STREAM_ID_NONE = -1;

  boolean startOfMessage();

  boolean endOfMessage();
}
