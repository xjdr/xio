package com.xjeffrose.xio.http;

import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.Queue;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Http2To1ProxyRequestQueue {

  private LinkedHashMap<Integer, Queue<PendingRequest>> streamQueue = Maps.newLinkedHashMap();
  private Response currentResponse = null;

  // region public functions

  public Optional<Integer> currentProxiedH2StreamId() {
    return streamQueue.keySet().stream().findFirst();
  }

  public Optional<Response> currentResponse() {
    return Optional.ofNullable(currentResponse);
  }

  public void onResponseDrainNext(ChannelHandlerContext ctx, Response response) {
    if (response.startOfMessage()) {
      currentResponse = response;
    }
    if (response.endOfMessage()) {
      streamQueue.remove(response.streamId());
      nextStreamsQueue()
          .ifPresent(
              queue -> {
                while (!queue.isEmpty()) {
                  PendingRequest pending = queue.remove();
                  log.debug("writing enqueued h2-h1 proxy request {}", pending.request);
                  if (isEmpty()) {
                    ctx.writeAndFlush(pending.request, pending.promise);
                  } else {
                    ctx.write(pending.request, pending.promise);
                  }
                }
              });
    }
  }

  public void onRequestWriteOrEnqueue(
      ChannelHandlerContext ctx, Integer streamId, Object request, ChannelPromise promise) {
    if (streamId == null || streamId == Message.H1_STREAM_ID_NONE) {
      log.debug("writing request {}", request);
      ctx.write(request, promise);
    } else {
      boolean shouldWrite =
          currentProxiedH2StreamId().map(id -> id.equals(streamId)).orElse(Boolean.TRUE);

      Queue<PendingRequest> queue =
          streamQueue.computeIfAbsent(streamId, k -> Queues.newArrayDeque());

      if (shouldWrite) {
        log.debug("writing h2-h1 proxy request {}", request);
        ctx.write(request, promise);
      } else {
        log.debug("enqueuing h2-h1 proxy request {}", request);
        queue.offer(new PendingRequest(request, promise));
      }
    }
  }

  public boolean isEmpty() {
    return streamQueue
        .values()
        .stream()
        .filter(queue -> !queue.isEmpty())
        .findFirst()
        .map(ignored -> Boolean.FALSE)
        .orElse(Boolean.TRUE);
  }

  // endregion

  // region private functions

  private Optional<Queue<PendingRequest>> nextStreamsQueue() {
    return streamQueue.values().stream().filter(queue -> !queue.isEmpty()).findFirst();
  }

  // endregion

  private static class PendingRequest {
    final Object request;
    final ChannelPromise promise;

    public PendingRequest(Object request, ChannelPromise promise) {
      this.request = request;
      this.promise = promise;
    }
  }
}
