package com.xjeffrose.xio.client;


import io.airlift.units.Duration;
import javax.annotation.Nullable;
import org.jboss.netty.channel.Channel;

public interface XioClientChannel extends RequestChannel {
  /**
   * Returns the timeout most recently set by {@link XioClientChannel#setSendTimeout(io.airlift.units.Duration)}
   */
  Duration getSendTimeout();

  /**
   * Sets a timeout used to limit elapsed time for sending a message.
   */
  void setSendTimeout(@Nullable Duration sendTimeout);

  /**
   * Returns the timeout most recently set by {@link XioClientChannel#setReceiveTimeout(io.airlift.units.Duration)}
   */
  Duration getReceiveTimeout();

  /**
   * Sets a timeout used to limit elapsed time between successful send, and reception of the
   * response.
   */
  void setReceiveTimeout(@Nullable Duration receiveTimeout);

  /**
   * Returns the timeout most recently set by {@link XioClientChannel#setReadTimeout(io.airlift.units.Duration)}
   */
  Duration getReadTimeout();

  /**
   * Sets a timeout used to limit the time that the client waits for data to be sent by the server.
   */
  void setReadTimeout(@Nullable Duration readTimeout);

  /**
   * Executes the given {@link Runnable} on the I/O thread that manages reads/writes for this
   * channel.
   */
  void executeInIoThread(Runnable runnable);

  Channel getNettyChannel();
}