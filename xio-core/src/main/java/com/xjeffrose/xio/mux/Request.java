package com.xjeffrose.xio.mux;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Request {
  public enum State {
    Prepare,
    Written,
    WaitForResponse,
    Complete;
  }

  @Getter private final UUID id;

  final SettableFuture<UUID> writeFuture;
  final Optional<SettableFuture<Response>> maybeResponseFuture;

  public boolean expectsResponse() {
    return maybeResponseFuture.isPresent();
  }

  public void registerResponseCallback(FutureCallback<Response> callback) {
    registerResponseCallback(callback, MoreExecutors.directExecutor());
  }

  public void registerResponseCallback(FutureCallback<Response> callback, Executor executor) {
    if (maybeResponseFuture.isPresent()) {
      Futures.addCallback(maybeResponseFuture.get(), callback, executor);
    } else {
      callback.onFailure(new RuntimeException("Request does not expect a Response"));
    }
  }

  public ListenableFuture<UUID> getWriteFuture() {
    return writeFuture;
  }

  public SettableFuture<UUID> getWritePromise() {
    return writeFuture;
  }

  public ListenableFuture<Response> getResponseFuture() {
    return maybeResponseFuture.get();
  }

  public SettableFuture<Response> getResponsePromise() {
    return maybeResponseFuture.get();
  }

  public Request(
      UUID id,
      SettableFuture<UUID> writeFuture,
      Optional<SettableFuture<Response>> maybeResponseFuture) {
    this.id = id;
    this.writeFuture = writeFuture;
    this.maybeResponseFuture = maybeResponseFuture;
  }

  public Request(
      UUID id, SettableFuture<UUID> writeFuture, SettableFuture<Response> responseFuture) {
    this(id, writeFuture, Optional.of(responseFuture));
  }

  public Request(UUID id, SettableFuture<UUID> writeFuture) {
    this(id, writeFuture, Optional.empty());
  }
}
