package com.xjeffrose.xio.http;

import com.xjeffrose.xio.tracing.XioTracing;
import io.netty.channel.*;
import io.netty.util.concurrent.Future;
import java.net.InetSocketAddress;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Client {
 // private Queue<ClientPayload> requestQueue = new ConcurrentLinkedQueue();
  Queue<ClientPayload> requestQueue = new ArrayDeque<>();
  private ChannelFuture connectFuture;
  private ChannelFutureListener writeListener;
  private ClientConnectionManager manager;
  private ClientState clientState;
  private ClientChannelInitializer channelInitializer;

  public Client(ClientState state, Supplier<ChannelHandler> appHandler, XioTracing tracing) {
    this.clientState = state;
    this.channelInitializer = new ClientChannelInitializer(state, appHandler, tracing);
    this.manager = new ClientConnectionManager(state, this.channelInitializer);
    writeListener =
      f -> {
        if (f.isDone() && f.isSuccess()) {
          log.debug("Write succeeded");
        } else {
          log.debug("Write failed", f.cause());
          if (manager.currentChannel() != null) {
            log.debug("pipeline: {}", manager.currentChannel().pipeline());
          }
        }
      };
  }

  public InetSocketAddress remoteAddress() {
    return clientState.remote;
  }

  /**
   * Creates a channel and returns a channel connection future This also sets the channel instance
   * variable so if we later call write it will not try to reconnect
   *
   * @return A ChannelFuture that succeeds on connect
   */

  /**
   * Combines the connection and writing into one command. This method dispatches both a connect and
   * command call concurrently. If there is already an existing channel we just do the write
   *
   * @param request The Request object that we ultimately want to send outbound
   * @return A ChannelFuture that succeeds when both the connect and write succeed
   */
  public ChannelFuture write(Request request) {
    ChannelPromise promise;
    if (manager.connectionState() == ClientConnectionState.NOT_CONNECTED) {
      // If we are not in a connected state we should buffer the requests until we find out
      // what happened to the connection try.  The connectFuture calls back on the same eventloop
      log.debug("== No channel exists, lets connect on client: " + this + " with request: " + request);
      ChannelFuture connectFuture = manager.connect();
      promise = manager.currentChannel().newPromise();
      log.debug("== Adding req: " + request + " to queue on client: " + this);
      this.requestQueue.add(new Client.ClientPayload(request, promise));
      connectFuture.addListener((connectionResult) -> {
        executeBufferedRequests(connectionResult);
      });
      return promise;
    } else if (manager.connectionState() == ClientConnectionState.CONNECTING) {
      // we are in the middle of connecting so lets just add to the queue
      // this is a non concurrent queue because these write calls methods will be called on the
      // same eventloop as the connectFuture.listener callback
      promise = manager.currentChannel().newPromise();
      log.debug("== Adding req: " + request + " to queue on client: " + this);
      this.requestQueue.add(new Client.ClientPayload(request, promise));
      return promise;
    } else if (manager.connectionState() == ClientConnectionState.CONNECTED) {
      // we are already connected so fire away
      log.debug("== already connected, just writing req: " + request + " on client: " + this);
      return this.rawWrite(request);
    } else {
      // Right now when a connection fails we don't retry blindly, we need to do something smarter so
      // we do not bombard the origin server with infinite retry. the channel should still exist so we shouldn't
      // have issues creating a newFailedFuture
      log.debug("== Connect failed on client: " + this);
      return manager.currentChannel().newFailedFuture(this.connectFuture.cause());
    }
  }

  private ChannelFuture rawWrite(Request request) {
    return request.endOfMessage() ? manager.currentChannel().writeAndFlush(request).addListener(this.writeListener) : manager.currentChannel().write(request).addListener(this.writeListener);
  }

  private void executeBufferedRequests(Future<? super Void> connectionResult) {
    boolean connectionSuccess = connectionResult.isDone() && connectionResult.isSuccess();
    log.debug("== Connection success was " + connectionSuccess);
    // loop through the queue until it's empty and fire away
    // this will happen on the same eventloop as the write so we don't need to worry about
    // trying to write to this queue at the same time we are dequeing
    while(!requestQueue.isEmpty()) {
      Client.ClientPayload pair = (Client.ClientPayload)requestQueue.remove();
      log.debug("== Dequeue req: " + pair.request + " on client: " + this);
      if (connectionSuccess) {
        this.rawWrite(pair.request).addListener((writeResult) -> {
          if (writeResult.isDone() && writeResult.isSuccess()) {
            log.debug("== Req: " + pair.request + " succeeded on client: " + this);
            pair.promise.setSuccess();
          } else {
            log.debug("== Req: " + pair.request + " failed on client: " + this);
            pair.promise.setFailure(connectionResult.cause());
          }
        });
      } else {
        pair.promise.setFailure(connectionResult.cause());
      }
    }
  }

  public void prepareForReuse(Supplier<ChannelHandler> handlerSupplier) {
    channelInitializer.setAppHandler(handlerSupplier);
    if (manager.currentChannel() != null) {
      manager.currentChannel().pipeline().addLast(ClientChannelInitializer.APP_HANDLER, handlerSupplier.get());
    }
  }

  public void recycle() {
    if (manager.currentChannel() != null) {
      manager.currentChannel().pipeline().remove(ClientChannelInitializer.APP_HANDLER);
      Http2ClientStreamMapper.http2ClientStreamMapper(manager.currentChannel().pipeline().firstContext()).clear();
    }
  }

  private class ClientPayload {
    public final Request request;
    public final ChannelPromise promise;

    public ClientPayload(Request request, ChannelPromise promise) {
      this.request = request;
      this.promise = promise;
    }
  }
}
