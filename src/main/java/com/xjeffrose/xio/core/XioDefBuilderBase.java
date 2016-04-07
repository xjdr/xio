package com.xjeffrose.xio.core;


import com.xjeffrose.xio.client.loadbalancer.Distributor;
import com.xjeffrose.xio.client.loadbalancer.XioDistributorFactory;
import com.xjeffrose.xio.processor.XioProcessorFactory;
import com.xjeffrose.xio.server.XioServerDef;
import io.airlift.units.Duration;
import java.net.InetSocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.Preconditions.checkState;

public abstract class XioDefBuilderBase<T extends XioDefBuilderBase<T>> {
  private static final AtomicInteger ID = new AtomicInteger(1);
  /**
   * The default maximum allowable size for a single incoming Http request or outgoing Http
   * response. A server can configure the actual maximum to be much higher (up to 0x7FFFFFFF or
   * almost 2 GB). This default could also be safely bumped up, but 64MB is chosen simply because it
   * seems reasonable that if you are sending requests or responses larger than that, it should be a
   * conscious decision (something you must manually configure).
   */
  private static final int MAX_FRAME_SIZE = 64 * 1024 * 1024;
  private int port;
  private int maxFrameSize;
  private int maxConnections;
  private int queuedResponseLimit;
  private XioProcessorFactory xioProcessorFactory;
  private Executor executor;
  private String name = "Xio-" + ID.getAndIncrement();
  private Duration clientIdleTimeout;
  private Duration taskTimeout;
  private XioSecurityFactory securityFactory;
  private InetSocketAddress hostAddress;
  private XioCodecFactory codecFactory;
  private XioAggregatorFactory aggregatorFactory;
  private XioRoutingFilterFactory routingFilterFactory;
  private Distributor distributor;

  public XioDefBuilderBase() {
    this.port = 8080;
    this.hostAddress = new InetSocketAddress("0.0.0.0", port);
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
    this.codecFactory = null;
    this.aggregatorFactory = null;
    this.routingFilterFactory = null;
  }

  public T name(String name) {
    this.name = name;
    return (T) this;
  }

  public T listen(int serverPort) {
    this.port = serverPort;
    return (T) this;
  }

  public T listen(InetSocketAddress addr) {
    this.port = addr.getPort();
    this.hostAddress = addr;
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

  public T withCodecFactory(XioCodecFactory codec) {
    this.codecFactory = codec;
    return (T) this;
  }

  public T withAggregator(XioAggregatorFactory aggregator) {
    this.aggregatorFactory = aggregator;
    return (T) this;
  }

  public T withRoutingFilter(XioRoutingFilterFactory routingFilterFactory) {
    this.routingFilterFactory = routingFilterFactory;
    return (T) this;
  }

  public T withDistributor(Distributor distributor) {
    this.distributor = distributor;
    return (T) this;
  }

  public XioServerDef build() {
    checkState(xioProcessorFactory != null, "Processor not defined!");
    checkState(codecFactory != null, "Codec not defined!");
    checkState(aggregatorFactory != null, "Aggregator not defined!");
    checkState(routingFilterFactory != null, "routingFilterFactory not defined!");

//    checkState(xioProcessorFactory == null, "Processors will be automatically adapted to XioProcessors, don't specify both");
    checkState(maxConnections >= 0, "maxConnections should be 0 (for unlimited) or positive");

    return new XioServerDef(
        name,
        port,
        hostAddress,
        maxFrameSize,
        queuedResponseLimit,
        maxConnections,
        xioProcessorFactory,
        clientIdleTimeout,
        taskTimeout,
        executor,
        securityFactory,
        codecFactory,
        aggregatorFactory,
        routingFilterFactory);
  }
}