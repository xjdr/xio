package com.xjeffrose.xio.core;


import com.google.common.util.concurrent.ListenableFuture;
import com.xjeffrose.xio.processor.XioProcessor;
import com.xjeffrose.xio.processor.XioProcessorFactory;
import io.airlift.units.Duration;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.HttpMessage;
import org.jboss.netty.handler.codec.http.HttpRequest;

import static com.google.common.base.Preconditions.checkState;

public abstract class HttpServerDefBuilderBase<T extends HttpServerDefBuilderBase<T>> {
  private static final AtomicInteger ID = new AtomicInteger(1);
  /**
   * The default maximum allowable size for a single incoming Http request or outgoing Http
   * response. A server can configure the actual maximum to be much higher (up to 0x7FFFFFFF or
   * almost 2 GB). This default could also be safely bumped up, but 64MB is chosen simply because it
   * seems reasonable that if you are sending requests or responses larger than that, it should be a
   * conscious decision (something you must manually configure).
   */
  private static final int MAX_FRAME_SIZE = 64 * 1024 * 1024;
  private int serverPort;
  private int maxFrameSize;
  private int maxConnections;
  private int queuedResponseLimit;
  private XioProcessorFactory xioProcessorFactory;
  private Executor executor;
  private String name = "Xio-" + ID.getAndIncrement();
  private Duration clientIdleTimeout;
  private Duration taskTimeout;
  private XioSecurityFactory securityFactory;

  public HttpServerDefBuilderBase() {
    this.serverPort = 8080;
    this.maxFrameSize = MAX_FRAME_SIZE;
    this.maxConnections = 0;
    this.queuedResponseLimit = 16;
    this.executor = new Executor() {
      @Override
      public void execute(Runnable runnable) {
        runnable.run();
      }
    };
    this.clientIdleTimeout = null;
    this.taskTimeout = null;
    this.securityFactory = new XioNoOpSecurityFactory();
  }

  public T name(String name) {
    this.name = name;
    return (T) this;
  }

  public T listen(int serverPort) {
    this.serverPort = serverPort;
    return (T) this;
  }

  public T withProcessorFactory(XioProcessorFactory processorFactory) {
    this.xioProcessorFactory = processorFactory;
    return (T) this;
  }

  public T limitFrameSizeTo(int maxFrameSize) {
    this.maxFrameSize = maxFrameSize;
    return (T) this;
  }

  public T limitConnectionsTo(int maxConnections) {
    this.maxConnections = maxConnections;
    return (T) this;
  }

  public T limitQueuedResponsesPerConnection(int queuedResponseLimit) {
    this.queuedResponseLimit = queuedResponseLimit;
    return (T) this;
  }

  public T clientIdleTimeout(Duration clientIdleTimeout) {
    this.clientIdleTimeout = clientIdleTimeout;
    return (T) this;
  }

  public T taskTimeout(Duration taskTimeout) {
    this.taskTimeout = taskTimeout;
    return (T) this;
  }

  public T using(Executor exe) {
    this.executor = exe;
    return (T) this;
  }

  public T withSecurityFactory(XioSecurityFactory securityFactory) {
    this.securityFactory = securityFactory;
    return (T) this;
  }

  public HttpServerDef build() {
//    checkState(xioProcessorFactory != null || HttpProcessorFactory != null, "Processor not defined!");
//    checkState(xioProcessorFactory == null || HttpProcessorFactory == null, "TProcessors will be automatically adapted to XioProcessors, don't specify both");
//    checkState(maxConnections >= 0, "maxConnections should be 0 (for unlimited) or positive");

    //TODO: Need a substantially more sane solution
    if (xioProcessorFactory == null) {
      xioProcessorFactory = new XioProcessorFactory() {
        @Override
        public XioProcessor getProcessor() {
          return new XioProcessor() {
            @Override
            public ListenableFuture<Boolean> process(ChannelHandlerContext ctx, HttpRequest in, RequestContext out, Map<Integer, HttpMessage> responseMap) {
              return null;
            }
          };
        }
      };
    }

    return new HttpServerDef(
        name,
        serverPort,
        maxFrameSize,
        queuedResponseLimit,
        maxConnections,
        xioProcessorFactory,
        clientIdleTimeout,
        taskTimeout,
        executor,
        securityFactory);
  }
}