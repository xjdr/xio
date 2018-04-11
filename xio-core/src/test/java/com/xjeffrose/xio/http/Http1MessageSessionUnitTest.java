package com.xjeffrose.xio.http;

import static io.netty.handler.codec.http.HttpMethod.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.*;

import io.netty.buffer.Unpooled;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class Http1MessageSessionUnitTest extends Assert {

  Http1MessageSession session;

  @Before
  public void setUp() {
    session = new Http1MessageSession();
  }

  @Test
  public void testOnRequestFull() {
    Request request =
        DefaultFullRequest.builder()
            .body(Unpooled.EMPTY_BUFFER)
            .headers(new DefaultHeaders())
            .method(GET)
            .path("/")
            .build();
    session.onRequest(request);

    assertTrue(session.initialRequest().requestFinished);
    assertFalse(session.initialRequest().responseFinished);
  }

  @Test
  public void testOnRequestStreaming() {
    Request request =
        DefaultStreamingRequest.builder()
            .headers(new DefaultHeaders())
            .method(GET)
            .path("/")
            .build();
    session.onRequest(request);

    assertFalse(session.initialRequest().requestFinished);
    assertFalse(session.initialRequest().responseFinished);
  }

  @Test
  public void testOnRequestPipeline() {
    Request request =
        DefaultFullRequest.builder()
            .body(Unpooled.EMPTY_BUFFER)
            .headers(new DefaultHeaders())
            .method(GET)
            .path("/")
            .build();
    session.onRequest(request);

    assertTrue(session.initialRequest().requestFinished);
    assertFalse(session.initialRequest().responseFinished);
    assertFalse(session.closeConnection());

    session.onRequest(request);
    assertTrue(session.closeConnection());
  }

  @Test
  public void testOnRequestData() {
    Request request =
        DefaultStreamingRequest.builder()
            .headers(new DefaultHeaders())
            .method(GET)
            .path("/")
            .build();
    session.onRequest(request);

    assertFalse(session.initialRequest().requestFinished);
    assertFalse(session.initialRequest().responseFinished);
    assertFalse(session.closeConnection());

    StreamingData data =
        DefaultStreamingData.builder()
            .content(Unpooled.EMPTY_BUFFER)
            .endOfMessage(true)
            .trailingHeaders(new DefaultHeaders())
            .build();
    session.onRequestData(data);
    assertTrue(session.initialRequest().requestFinished);
  }

  @Test
  public void testOnResponseFull() {
    Request request =
        DefaultFullRequest.builder()
            .body(Unpooled.EMPTY_BUFFER)
            .headers(new DefaultHeaders())
            .method(GET)
            .path("/")
            .build();
    session.onRequest(request);

    assertTrue(session.initialRequest().requestFinished);
    assertFalse(session.initialRequest().responseFinished);

    Response response =
        DefaultFullResponse.builder()
            .body(Unpooled.EMPTY_BUFFER)
            .status(OK)
            .headers(new DefaultHeaders())
            .build();
    session.onResponse(response);
    assertTrue(session.initialRequest().requestFinished);
    assertTrue(session.initialRequest().responseFinished);
  }

  @Test
  public void testOnResponseStreaming() {
    Request request =
        DefaultFullRequest.builder()
            .body(Unpooled.EMPTY_BUFFER)
            .headers(new DefaultHeaders())
            .method(GET)
            .path("/")
            .build();
    session.onRequest(request);

    assertTrue(session.initialRequest().requestFinished);
    assertFalse(session.initialRequest().responseFinished);

    Response response =
        DefaultStreamingResponse.builder().status(OK).headers(new DefaultHeaders()).build();
    session.onResponse(response);
    assertTrue(session.initialRequest().requestFinished);
    assertFalse(session.initialRequest().responseFinished);
  }

  @Test
  public void testOnResponseData() {
    Request request =
        DefaultFullRequest.builder()
            .body(Unpooled.EMPTY_BUFFER)
            .headers(new DefaultHeaders())
            .method(GET)
            .path("/")
            .build();
    session.onRequest(request);

    assertTrue(session.initialRequest().requestFinished);
    assertFalse(session.initialRequest().responseFinished);

    Response response =
        DefaultStreamingResponse.builder().status(OK).headers(new DefaultHeaders()).build();
    session.onResponse(response);
    assertTrue(session.initialRequest().requestFinished);
    assertFalse(session.initialRequest().responseFinished);

    StreamingData data =
        DefaultStreamingData.builder()
            .content(Unpooled.EMPTY_BUFFER)
            .endOfMessage(true)
            .trailingHeaders(new DefaultHeaders())
            .build();
    session.onResponseData(data);
    assertTrue(session.initialRequest().requestFinished);
    assertTrue(session.initialRequest().responseFinished);
  }

  @Test
  public void testFlush() {
    Request request =
        DefaultFullRequest.builder()
            .body(Unpooled.EMPTY_BUFFER)
            .headers(new DefaultHeaders())
            .method(GET)
            .path("/")
            .build();
    session.onRequest(request);

    assertTrue(session.initialRequest().requestFinished);
    assertFalse(session.initialRequest().responseFinished);
    session.flush();
    assertNotNull(session.currentRequest());

    Response response =
        DefaultFullResponse.builder()
            .body(Unpooled.EMPTY_BUFFER)
            .status(OK)
            .headers(new DefaultHeaders())
            .build();
    session.onResponse(response);
    assertTrue(session.initialRequest().requestFinished);
    assertTrue(session.initialRequest().responseFinished);
    session.flush();
    assertNull(session.currentRequest());
  }
}
