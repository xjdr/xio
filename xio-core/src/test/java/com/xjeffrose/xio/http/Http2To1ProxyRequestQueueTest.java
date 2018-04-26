package com.xjeffrose.xio.http;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.xjeffrose.xio.http.internal.Http1SegmentedData;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class Http2To1ProxyRequestQueueTest extends Assert {

  @Mock ChannelHandlerContext mockCtx;
  private Http2To1ProxyRequestQueue subject;

  @Before
  public void beforeEach() {
    MockitoAnnotations.initMocks(this);
    subject = new Http2To1ProxyRequestQueue();
  }

  @Test
  public void testInterleavedH2FrontRequests_stateTransitions() {
    // should start empty
    assertTrue(subject.isEmpty());

    // the first stream id should write
    subject.onRequestWriteOrEnqueue(mockCtx, 3, "request 1a", mock(ChannelPromise.class));
    verify(mockCtx).write(eq("request 1a"), any());
    assertTrue(subject.isEmpty());

    // the first stream id should write again
    subject.onRequestWriteOrEnqueue(mockCtx, 3, "request 1b", mock(ChannelPromise.class));
    verify(mockCtx).write(eq("request 1b"), any());
    assertTrue(subject.isEmpty());

    // subsequent stream ids should be enqueued
    subject.onRequestWriteOrEnqueue(mockCtx, 5, "request 2a", mock(ChannelPromise.class));
    verify(mockCtx, never()).write(eq("request 2a"), any());
    assertFalse(subject.isEmpty());

    // the first stream id should write again
    subject.onRequestWriteOrEnqueue(mockCtx, 3, "request 1c", mock(ChannelPromise.class));
    verify(mockCtx).write(eq("request 1c"), any());
    assertFalse(subject.isEmpty());

    // when the first stream id response completes
    assertEquals(3, subject.currentProxiedH2StreamId().orElse(0), 0);
    Response response =
        DefaultFullResponse.builder()
            .headers(new DefaultHeaders())
            .status(HttpResponseStatus.OK)
            .body(Unpooled.EMPTY_BUFFER)
            .streamId(3)
            .build();
    subject.onResponseDrainNext(mockCtx, response);

    // then the subsequent stream ids should write
    verify(mockCtx).writeAndFlush(eq("request 2a"), any());
    assertSame(response, subject.currentResponse().orElse(null));

    // and the stream id should be correct
    assertEquals(5, subject.currentProxiedH2StreamId().orElse(0), 0);

    // and the the queue should be empty
    assertTrue(subject.isEmpty());
  }

  @Test
  public void testInterleavedH2FrontRequestsSegmentedResponse_stateTransitions() {
    // should start empty
    assertTrue(subject.isEmpty());

    // the first stream id should write
    subject.onRequestWriteOrEnqueue(mockCtx, 3, "request 1a", mock(ChannelPromise.class));
    verify(mockCtx).write(eq("request 1a"), any());
    assertTrue(subject.isEmpty());

    // the first stream id should write again
    subject.onRequestWriteOrEnqueue(mockCtx, 3, "request 1b", mock(ChannelPromise.class));
    verify(mockCtx).write(eq("request 1b"), any());
    assertTrue(subject.isEmpty());

    // subsequent stream ids should be enqueued (and not written)
    subject.onRequestWriteOrEnqueue(mockCtx, 5, "request 2a", mock(ChannelPromise.class));
    verify(mockCtx, never()).write(eq("request 2a"), any());
    assertFalse(subject.isEmpty());

    // the first stream id should write again
    subject.onRequestWriteOrEnqueue(mockCtx, 3, "request 1c", mock(ChannelPromise.class));
    verify(mockCtx).write(eq("request 1c"), any());
    assertFalse(subject.isEmpty());

    // subsequent stream ids should be enqueued (and not written)
    subject.onRequestWriteOrEnqueue(mockCtx, 5, "request 2b", mock(ChannelPromise.class));
    verify(mockCtx, never()).write(eq("request 2b"), any());
    assertFalse(subject.isEmpty());

    //////////// when the first stream id responses occur
    assertEquals(3, subject.currentProxiedH2StreamId().orElse(0), 0);
    Response stream3Response0 =
        DefaultSegmentedResponse.builder()
            .headers(new DefaultHeaders())
            .status(HttpResponseStatus.OK)
            .streamId(3)
            .build();

    subject.onResponseDrainNext(mockCtx, stream3Response0);
    assertSame(stream3Response0, subject.currentResponse().orElse(null));
    verifyNoMoreInteractions(mockCtx); // should not write anything queued

    Response stream3Response1 =
        new SegmentedResponseData(
            stream3Response0,
            new Http1SegmentedData(new DefaultHttpContent(Unpooled.EMPTY_BUFFER)));

    subject.onResponseDrainNext(mockCtx, stream3Response1);
    assertSame(stream3Response1, subject.currentResponse().orElse(null));
    verifyNoMoreInteractions(mockCtx); // should not write anything queued

    // when the first stream id responses complete
    Response stream3Response2 =
        new SegmentedResponseData(
            stream3Response0,
            new Http1SegmentedData(new DefaultLastHttpContent(Unpooled.EMPTY_BUFFER)));

    subject.onResponseDrainNext(mockCtx, stream3Response2);
    assertSame(stream3Response1, subject.currentResponse().orElse(null));

    // then the second stream's enqueued requests should spool out
    verify(mockCtx).write(eq("request 2a"), any()); // should write stream 5 requests
    verify(mockCtx).writeAndFlush(eq("request 2b"), any()); // should write stream 5 requests

    //////////// when the second stream id responses occur

    assertEquals(5, subject.currentProxiedH2StreamId().orElse(0), 0);
    Response stream5Response0 =
        DefaultSegmentedResponse.builder()
            .headers(new DefaultHeaders())
            .status(HttpResponseStatus.OK)
            .streamId(5)
            .build();

    subject.onResponseDrainNext(mockCtx, stream5Response0);
    assertSame(stream5Response0, subject.currentResponse().orElse(null));

    Response stream5Response1 =
        new SegmentedResponseData(
            stream5Response0,
            new Http1SegmentedData(new DefaultHttpContent(Unpooled.EMPTY_BUFFER)));

    subject.onResponseDrainNext(mockCtx, stream5Response1);
    assertSame(stream5Response1, subject.currentResponse().orElse(null));

    Response stream5Response2 =
        new SegmentedResponseData(
            stream5Response0,
            new Http1SegmentedData(new DefaultLastHttpContent(Unpooled.EMPTY_BUFFER)));

    subject.onResponseDrainNext(mockCtx, stream5Response2);
    assertSame(stream5Response1, subject.currentResponse().orElse(null));

    // and the the queue should be empty
    assertTrue(subject.isEmpty());
  }
}
