package com.xjeffrose.xio.client;

import java.util.UUID;

public interface XioListener<T> {

  void onRequestSent();

  void onResponseReceived(T message, boolean success);

  void onChannelError(Exception requestException) throws XioClientException;

  T getResponse(UUID id) throws XioClientTimeoutException;

  boolean getStatus(UUID id) throws XioClientTimeoutException;

  void onChannelReadComplete();

  void addID(UUID id);

  void removeID(UUID id);
}
