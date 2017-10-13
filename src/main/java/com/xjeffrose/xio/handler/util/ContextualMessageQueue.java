package com.xjeffrose.xio.handler.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Queue up contextualMessages until the queue starts streaming, then consume queued
 * contextualMessages and allow all future contextualMessages to pass straight to the consumer. <p>
 * Barrier is re-usable and can be reset to support Keep-Alive
 *
 * @param <C> the type for the context for the message
 * @param <M> the type of the message being added to the queue
 */
public class ContextualMessageQueue<C, M> {

  private final BiConsumer<C, M> consumer;
  //  private final List<ContextualMessage> contextualMessages = new ArrayList<>();
  // AD: Prior to netty 4.1.14, the Recycler allowed the same CodecOutputList object to be used in testing so having
  // a context attached to each message worked.  In reality there is no expectation of having the same CodecOutputList
  // for each decode call.  The context we are interested in when we startStreaming is the most recent one.
  private final List<M> messages = new ArrayList<>();
  private C context;
  private boolean streaming = false;

  /**
   * Registers a consumer with this barrier.
   *
   * The consumer will not receive any contextualMessages until the queue begins streaming.
   *
   * @param consumer a BiConsumer that will be fed each context and message pair once streaming
   * starts.
   */
  public ContextualMessageQueue(BiConsumer<C, M> consumer) {
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
  public boolean addContextualMessage(C ctx, M msg) {
    if (streaming) {
      if (!messages.isEmpty()) {
        throw new IllegalStateException("Messages found in the queue while streaming.");
      }
      consumer.accept(ctx, msg);
    } else {
      // Set context to most recent context.  When startStreaming is called it will consume these messages using
      // the most recent context.  This is important as the CodecOutputList is new for each decode call, meaning there
      // will be different contexts for a given channel pipeline.  When streaming is started, the older contexts are
      // no longer relevant.  Only the most recent one is.
      context = ctx;
      messages.add(msg);
    }
    return streaming;
  }

  /**
   * Clears all contextualMessages and resets to non-streaming mode.
   */
  public void reset() {
    messages.clear();
    streaming = false;
  }

  /**
   * Allows a consumer to consume the stream of queued up contextualMessages.
   *
   * Using forEach will not clear or reset the queue.
   *
   * @param consumer a BiConsumer that will be fed each context and message pair.
   */
  public void forEach(BiConsumer<C, M> consumer) {
    messages.stream().forEach(msg -> consumer.accept(context, msg));
  }

  /**
   * Allows a consumer to consume the stream of queued up messages without their context.
   *
   * @param consumer a Consumer that will be fed each message.
   */
  public void forEachMessage(Consumer<M> consumer) {
    messages.stream().forEach(msg -> consumer.accept(msg));
  }

  /**
   * Initiates streaming mode and streams all queued up contextualMessages to the registered
   * consumer.
   */
  public void startStreaming() {
    forEach(consumer);
    messages.clear();
    streaming = true;
  }

  /**
   * Returns the number of queued contextualMessages.
   *
   * Once streaming begins the queue should remain empty.
   *
   * @return the number of queued contextualMessages.
   */
  public int size() {
    return messages.size();
  }

  /**
   * Check if the queue is in streaming mode.
   *
   * @return true if contextualMessages are being streamed to the consumer
   */
  public boolean isStreaming() {
    return streaming;
  }

//  private final class ContextualMessage {
//    private final M msg;
//    private final C ctx;
//
//    public ContextualMessage(C ctx, M msg) {
//      this.ctx = ctx;
//      this.msg = msg;
//    }
//  }
}
