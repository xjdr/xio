package com.xjeffrose.xio.client;

import org.jboss.netty.buffer.ChannelBuffer;

public interface RequestChannel {
  /**
   * Sends a single message asynchronously, and notifies the {@link Listener} when the request is
   * finished sending, when the response has arrived, and/or when an error occurs.
   *
   * @throws XioException
   */
  void sendAsynchronousRequest(final ChannelBuffer request,
                               final boolean oneway,
                               final Listener listener)
      throws XioException;

  /**
   * Closes the channel
   */
  void close();

  /**
   * Checks whether the channel has encountered an error. This method is a shortcut for:
   *
   * <code> return (getError() != null); </code>
   *
   * @return {@code true} if the {@link RequestChannel} is broken
   */
  boolean hasError();

  /**
   * Returns the {@link XioException} representing the error the channel encountered, if any.
   *
   * @return An instance of {@link XioException} or {@code null} if the channel is still good.
   */
  XioException getError();

  /**
   * Returns the {@link TDuplexProtocolFactory} that should be used by clients code to serialize
   * messages for sending on this channel
   *
   * @return An instance of {@link TDuplexProtocolFactory}
   */
  XioProtocolFactory getProtocolFactory();

  /**
   * The listener interface that must be implemented for callback objects passed to {@link
   * #sendAsynchronousRequest}
   */
  public interface Listener {
    /**
     * This will be called when the request has successfully been written to the transport layer
     * (e.g. socket)
     */
    void onRequestSent();

    /**
     * This will be called when a full response to the request has been received
     *
     * @param message The response buffer
     */
    void onResponseReceived(ChannelBuffer message);

    /**
     * This will be called if the channel encounters an error before the request is sent or before a
     * response is received
     *
     * @param requesXioException A {@link XioException} describing the problem that was encountered
     */
    void onChannelError(XioException requestException);
  }
}