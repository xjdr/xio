package com.xjeffrose.xio.core;


import com.xjeffrose.xio.processor.XioProcessorFactory;
import io.airlift.units.Duration;
import java.util.concurrent.Executor;

/**
 * Descriptor for a Http Server. This defines a listener port that Xio need to start a Http
 * endpoint.
 */
public class HttpServerDef {
  private final int serverPort;
  private final int maxFrameSize;
  private final int maxConnections;
  private final int queuedResponseLimit;
  private final XioProcessorFactory processorFactory;
//  private final TDuplexProtocolFactory duplexProtocolFactory;

  private final Duration clientIdleTimeout;
  private final Duration taskTimeout;

  //  private final HttpFrameCodecFactory HttpFrameCodecFactory;
  private final Executor executor;
  private final String name;
  private final XioSecurityFactory securityFactory;

  public HttpServerDef(
      String name,
      int serverPort,
      int maxFrameSize,
      int queuedResponseLimit,
      int maxConnections,
      XioProcessorFactory processorFactory,
//      TDuplexProtocolFactory duplexProtocolFactory,
      Duration clientIdleTimeout,
      Duration taskTimeout,
//      HttpFrameCodecFactory HttpFrameCodecFactory,
      Executor executor,
      XioSecurityFactory securityFactory) {
    this.name = name;
    this.serverPort = serverPort;
    this.maxFrameSize = maxFrameSize;
    this.maxConnections = maxConnections;
    this.queuedResponseLimit = queuedResponseLimit;
    this.processorFactory = processorFactory;
//    this.duplexProtocolFactory = duplexProtocolFactory;
    this.clientIdleTimeout = clientIdleTimeout;
    this.taskTimeout = taskTimeout;
//    this.HttpFrameCodecFactory = HttpFrameCodecFactory;
    this.executor = executor;
    this.securityFactory = securityFactory;
  }

  public static HttpServerDefBuilder newBuilder() {
    return new HttpServerDefBuilder();
  }

  public int getServerPort() {
    return serverPort;
  }

  public int getMaxFrameSize() {
    return maxFrameSize;
  }

  public int getMaxConnections() {
    return maxConnections;
  }

  public int getQueuedResponseLimit() {
    return queuedResponseLimit;
  }

  public XioProcessorFactory getProcessorFactory() {
    return processorFactory;
  }

//  public TDuplexProtocolFactory getDuplexProtocolFactory()
//  {
//    return duplexProtocolFactory;
//  }

  public Duration getClientIdleTimeout() {
    return clientIdleTimeout;
  }

  public Duration getTaskTimeout() {
    return taskTimeout;
  }

  public Executor getExecutor() {
    return executor;
  }

  public String getName() {
    return name;
  }

//  public HttpFrameCodecFactory getHttpFrameCodecFactory()
//  {
//    return HttpFrameCodecFactory;
//  }

  public XioSecurityFactory getSecurityFactory() {
    return securityFactory;
  }
}