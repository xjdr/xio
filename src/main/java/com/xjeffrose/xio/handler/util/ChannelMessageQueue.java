package com.xjeffrose.xio.handler.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Queue up messages until the queue starts streaming, then consume queued messages and allow all
 * future messages to pass straight to the consumer. <p> Barrier is re-usable and can be reset to
 * support Keep-Alive
 *
 * @param <C> the type for the context for the message
 * @param <M> the type of the message being added to the queue
 */
public class ChannelMessageQueue<C, M> {

  private final BiConsumer<C, M> consumer;
  private final List<Message> messages = new ArrayList<>();
  private boolean streaming = false;

  /**
   * Registers a consumer with this barrier.
   *
   * The consumer will not receive any messages until the queue begins streaming.
   *
   * @param consumer a BiConsumer that will be fed each context and message pair once streaming
   * starts.
   */
  public ChannelMessageQueue(BiConsumer<C, M> consumer) {
    this.consumer = consumer;
  }

  /**
   * Will either queue up a message or provide it to the consumer depending on if we are in
   * streaming mode or not.
   *
   * @param ctx the context for the message being added
   * @param msg the message being added to the queue
   * @return boolean true if provided to consumer
   */
  public boolean addMessage(C ctx, M msg) {
    if (streaming) {
      consumer.accept(ctx, msg);
    } else {
      messages.add(new Message(ctx, msg));
    }
    return streaming;
  }

  /**
   * Clears all messages and resets to non-streaming mode.
   */
  public void reset() {
    messages.clear();
    streaming = false;
  }

  /**
   * Allows a consumer to traverse the stream of queued up messages.
   *
   * Using forEach will not clear or reset the queue.
   *
   * @param consumer a BiConsumer that will be fed each context and message pair.
   */
  public void forEach(BiConsumer<C, M> consumer) {
    messages.stream().forEach((msg) -> consumer.accept(msg.ctx, msg.msg));
  }

  /**
   * Initiates streaming mode and streams all queued up messages to the registered consumer.
   */
  public void startStreaming() {
    streaming = true;
    forEach(consumer);
    messages.clear();
  }

  /**
   * Returns the number of queued messages.
   *
   * Once streaming begins the queue should remain empty.
   *
   * @return the number of queued messages.
   */
  public int size() {
    return messages.size();
  }

  /**
   * Check if the queue is in streaming mode.
   *
   * @return true if messages are being streamed to the consumer
   */
  public boolean isStreaming() {
    return streaming;
  }

  private final class Message {
    private M msg;
    private C ctx;

    public Message(C ctx, M msg) {
      this.ctx = ctx;
      this.msg = msg;
    }
  }
}
