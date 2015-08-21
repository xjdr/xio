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

/**
 * Builder for the Http Server descriptor. Example : <code> new HttpServerDefBuilder()
 * .listen(config.getServerPort()) .limitFrameSizeTo(config.getMaxFrameSize()) .withProcessor(new
 * FacebookService.Processor(new MyFacebookBase())) .using(Executors.newFixedThreadPool(5))
 * .build(); </code> You can then pass HttpServerDef to guice via a multibinder.
 */
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
  //  private HttpFrameCodecFactory HttpFrameCodecFactory;
  private int serverPort;
  private int maxFrameSize;
  private int maxConnections;
  private int queuedResponseLimit;
  private XioProcessorFactory xioProcessorFactory;
  //  private TProcessorFactory HttpProcessorFactory;
//  private TDuplexProtocolFactory duplexProtocolFactory;
  private Executor executor;
  private String name = "Xio-" + ID.getAndIncrement();
  private Duration clientIdleTimeout;
  private Duration taskTimeout;
  private XioSecurityFactory securityFactory;

  /**
   * Create a HttpServerDefBuilder with common defaults
   */
  public HttpServerDefBuilderBase() {
    this.serverPort = 8080;
    this.maxFrameSize = MAX_FRAME_SIZE;
    this.maxConnections = 0;
    this.queuedResponseLimit = 16;
//    this.duplexProtocolFactory = TDuplexProtocolFactory.fromSingleFactory(new TBinaryProtocol.Factory(true, true));
    this.executor = new Executor() {
      @Override
      public void execute(Runnable runnable) {
        runnable.run();
      }
    };
    this.clientIdleTimeout = null;
    this.taskTimeout = null;
//    this.HttpFrameCodecFactory = new DefaultHttpFrameCodecFactory();
    this.securityFactory = new XioNoOpSecurityFactory();
  }

  /**
   * Give the endpoint a more meaningful name.
   */
  public T name(String name) {
    this.name = name;
    return (T) this;
  }

  /**
   * Listen to this port.
   */
  public T listen(int serverPort) {
    this.serverPort = serverPort;
    return (T) this;
  }

  /**
   * Specify protocolFactory for both input and output
   */
//  public T protocol(TDuplexProtocolFactory tProtocolFactory)
//  {
//    this.duplexProtocolFactory = tProtocolFactory;
//    return (T) this;
//  }
//
//  public T protocol(TProtocolFactory tProtocolFactory)
//  {
//    this.duplexProtocolFactory = TDuplexProtocolFactory.fromSingleFactory(tProtocolFactory);
//    return (T) this;
//  }

//  /**
//   * Specify the TProcessor.
//   */
//  public T withProcessor(final XioProcessor processor)
//  {
//    this.XioProcessorFactory = new XioProcessorFactory() {
//      @Override
//      public XioProcessor getProcessor(TTransport transport)
//      {
//        return processor;
//      }
//    };
//    return (T) this;
//  }
//
//  public T withProcessor(TProcessor processor)
//  {
//    this.HttpProcessorFactory = new TProcessorFactory(processor);
//    return (T) this;
//  }
//

  /**
   * Anohter way to specify the TProcessor.
   */
  public T withProcessorFactory(XioProcessorFactory processorFactory) {
    this.xioProcessorFactory = processorFactory;
    return (T) this;
  }
//
//  /**
//   * Anohter way to specify the TProcessor.
//   */
//  public T withProcessorFactory(TProcessorFactory processorFactory)
//  {
//    this.HttpProcessorFactory = processorFactory;
//    return (T) this;
//  }

  /**
   * Set frame size limit.  Default is MAX_FRAME_SIZE
   */
  public T limitFrameSizeTo(int maxFrameSize) {
    this.maxFrameSize = maxFrameSize;
    return (T) this;
  }

  /**
   * Set maximum number of connections. Default is 0 (unlimited)
   */
  public T limitConnectionsTo(int maxConnections) {
    this.maxConnections = maxConnections;
    return (T) this;
  }

  /**
   * Limit number of queued responses per connection, before pausing reads to catch up.
   */
  public T limitQueuedResponsesPerConnection(int queuedResponseLimit) {
    this.queuedResponseLimit = queuedResponseLimit;
    return (T) this;
  }

  /**
   * Specify timeout during which if connected client doesn't send a message, server will disconnect
   * the client
   */
  public T clientIdleTimeout(Duration clientIdleTimeout) {
    this.clientIdleTimeout = clientIdleTimeout;
    return (T) this;
  }

  /**
   * Specify timeout during which: 1. if a task remains on the executor queue, server will cancel
   * the task when it is dispatched. 2. if a task is scheduled but does not finish processing,
   * server will send timeout exception back.
   */
  public T taskTimeout(Duration taskTimeout) {
    this.taskTimeout = taskTimeout;
    return (T) this;
  }

//  public T HttpFrameCodecFactory(HttpFrameCodecFactory HttpFrameCodecFactory)
//  {
//    this.HttpFrameCodecFactory = HttpFrameCodecFactory;
//    return (T) this;
//  }

  /**
   * Specify an executor for Http processor invocations ( i.e. = THaHsServer ) By default invocation
   * happens in Netty single thread ( i.e. = TNonBlockingServer )
   */
  public T using(Executor exe) {
    this.executor = exe;
    return (T) this;
  }

  public T withSecurityFactory(XioSecurityFactory securityFactory) {
    this.securityFactory = securityFactory;
    return (T) this;
  }

  /**
   * Build the HttpServerDef
   */
  public HttpServerDef build() {
//    checkState(xioProcessorFactory != null || HttpProcessorFactory != null, "Processor not defined!");
//    checkState(xioProcessorFactory == null || HttpProcessorFactory == null, "TProcessors will be automatically adapted to XioProcessors, don't specify both");
//    checkState(maxConnections >= 0, "maxConnections should be 0 (for unlimited) or positive");

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
//        duplexProtocolFactory,
        clientIdleTimeout,
        taskTimeout,
//        HttpFrameCodecFactory,
        executor,
        securityFactory);
  }
}