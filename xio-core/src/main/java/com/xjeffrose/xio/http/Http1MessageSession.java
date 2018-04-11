package com.xjeffrose.xio.http;

import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;

// Fun reading!
// Pipelining: https://tools.ietf.org/html/rfc7230#section-6.3.2
// https://en.wikipedia.org/wiki/HTTP_pipelining
// https://developer.mozilla.org/en-US/docs/Web/HTTP/Connection_management_in_HTTP_1.x

// Scenarios
// 1: client sends all of request before we respond
// 2: client sends request1 and request2 before we respond (http pipelining)
// 3: client sends part of request, we respond in full, client sends the rest of the request
// 4: client sends part of the request, we respond in full, client sends nothing (need timeout to
// detect)

/**
 * Http1MessageSession is a finite state machine to track the current HTTP/1.1 message session. This
 * class exists to store the connection specific state of the message session, it should be attached
 * to the Channel via the attr method.
 */
@Slf4j
public class Http1MessageSession {
  /*
  This is an attempt at describing the state machine needed for doing
  http pipelining. We haven't implemented this but it serves as a
  document for why we're not supporting http pipelining.

  @startuml
  [*] -> NoMessages
  NoMessages -> InitialRequestReceived
  InitialRequestReceived -> InitialResponseSent
  InitialResponseSent -> NoMessages

  InitialRequestReceived -> PipelineRequestReceived
  PipelineRequestReceived -> PipelineRequestReceived
  PipelineRequestReceived -> PipelineLimitReached
  PipelineLimitReached -> NoMessages

  InitialResponseSent -> PipelineResponseSent
  PipelineResponseSent -> PipelineResponseSent
  PipelineResponseSent -> NoMessages
  @enduml
    */

  public static class RequestMeta {
    public final Request request;
    public boolean requestFinished;
    public boolean responseFinished;

    RequestMeta(Request request, boolean requestFinished) {
      this.request = request;
      this.requestFinished = requestFinished;
      responseFinished = false;
    }
  }

  // We can only handle one request at a time, any additional requests will be ignored.
  private RequestMeta initialRequest;
  // The client tried to send another request before the first request was responded to.
  private boolean clientTriedPipeline;

  @VisibleForTesting
  public RequestMeta initialRequest() {
    return initialRequest;
  }

  private void reset() {
    initialRequest = null;
    clientTriedPipeline = false;
  }

  public Http1MessageSession() {
    reset();
  }

  /**
   * Called when the client has sent a Request.
   *
   * @param request The Request object that the client has sent
   */
  public void onRequest(Request request) {
    if (initialRequest == null) {
      boolean fullRequest = (request instanceof FullRequest);
      initialRequest = new RequestMeta(request, fullRequest);
    } else {
      // log that the client is attempting to pipeline
      if (clientTriedPipeline == false) {
        // only log an error once
        log.error(
            "Client attempted to send Request before response finished (HTTP Pipelining): {}",
            request);
        clientTriedPipeline = true;
      } else {
        log.debug("Client attempted to send another pipelined request: {}", request);
      }
    }
  }

  /**
   * Called when the client has sent a StreamingData object as part of a Request.
   *
   * @param data The StreamingData object that the client has sent
   */
  public void onRequestData(StreamingData data) {
    if (initialRequest == null) {
      log.error("Received StreamingData without a current Request, dropping data: {}", data);
      return;
    }

    if (data.endOfMessage()) {
      initialRequest.requestFinished = true;
    }
  }

  /**
   * Called before sending a Response object to the client.
   *
   * @param response The Response object the server is about to send
   */
  public void onResponse(Response response) {
    if (initialRequest != null) {
      if (initialRequest.requestFinished == false) {
        // We are responding before the request has finished.
        // It's possible that we're being bad http1 actors by
        // responding before the request is finished sending, we should log
        // if/when that happens
        log.error(
            "Response is being sent before the request has finished processing: {}", response);
      }

      if (response instanceof FullResponse) {
        initialRequest.responseFinished = true;
      }
    }
  }

  /**
   * Called before a StreamingData object is sent to the client as part of a Response.
   *
   * @param data The StreamingData object the server is about to send
   */
  public void onResponseData(StreamingData data) {
    if (initialRequest != null) {
      if (data.endOfMessage()) {
        initialRequest.responseFinished = true;
      }
    } else {
      log.error(
          "Attempted to write StreamingData without a current Request, dropping data: {}", data);
    }
  }

  /**
   * Returns the Request object for the current session (if any).
   *
   * @return the current Request or null
   */
  public Request currentRequest() {
    if (initialRequest != null) {
      return initialRequest.request;
    }
    return null;
  }

  /**
   * Should the server close the connection after sending the response? Usually true if the client
   * has tried some unsupported action, like HTTP Pipelining.
   *
   * @return if the connection should be closed
   */
  public boolean closeConnection() {
    return clientTriedPipeline;
  }

  /**
   * Check if the message session has completed, if so remove state and prepare for the next
   * session.
   */
  public void flush() {
    // If we have a current request and we've seen all of the request data and all of the response
    // data we can reset.
    if (initialRequest != null
        && initialRequest.requestFinished
        && initialRequest.responseFinished) {
      reset();
    }
  }
}
