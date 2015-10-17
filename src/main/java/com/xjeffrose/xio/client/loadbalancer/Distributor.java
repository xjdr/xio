package com.xjeffrose.xio.client.loadbalancer;

import com.google.common.net.HostAndPort;
import com.google.common.util.concurrent.ListenableFuture;
import com.xjeffrose.xio.client.RetryPolicy;
import com.xjeffrose.xio.client.RetrySleeper;
import com.xjeffrose.xio.client.XioClientChannel;
import com.xjeffrose.xio.client.XioClientConfig;
import com.xjeffrose.xio.client.XioClientConnector;
import com.xjeffrose.xio.client.XioFuture;
import com.xjeffrose.xio.core.ShutdownUtil;
import io.airlift.units.Duration;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.Timer;
import java.io.Closeable;
import java.net.InetSocketAddress;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import org.apache.log4j.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Creates a new XioClient with defaults: cachedThreadPool for bossExecutor and workerExecutor
 */

public class Distributor implements Closeable {
  private static final Logger log = Logger.getLogger(Distributor.class);

  public static final Duration DEFAULT_CONNECT_TIMEOUT = new Duration(2, TimeUnit.SECONDS);
  public static final Duration DEFAULT_RECEIVE_TIMEOUT = new Duration(2, TimeUnit.SECONDS);
  public static final Duration DEFAULT_READ_TIMEOUT = new Duration(2, TimeUnit.SECONDS);
  private static final Duration DEFAULT_SEND_TIMEOUT = new Duration(2, TimeUnit.SECONDS);
  private static final int DEFAULT_MAX_FRAME_SIZE = 16777216;

  private final Vector<Node> nodes;
  private LoadBalancingStrategy strategy;
  private final XioClientConfig xioClientConfig;
  private final EventLoopGroup group;
  private final ChannelGroup allChannels;
  private final Timer timer;

  public Distributor(XioClientConfig xioClientConfig, Vector<Node> nodes, LoadBalancingStrategy strategy) {
    this.xioClientConfig = xioClientConfig;
    this.nodes = nodes;
    this.strategy = strategy;
    this.timer = xioClientConfig.getTimer();
    this.group = new NioEventLoopGroup(1);
    this.allChannels = new DefaultChannelGroup(this.group.next());
  }

  private static InetSocketAddress toInetAddress(HostAndPort hostAndPort) {
    return (hostAndPort == null) ? null : new InetSocketAddress(hostAndPort.getHostText(), hostAndPort.getPort());
  }

  /**
   * The vector of nodes over which we are currently balancing.
   */
  public Vector<Node> vector() {
    return nodes;
  }

  /**
   * Pick the next node. This is the main load balancer.
   */
  public Node pick() {
    return null;
  }

  /**
   * True if this distributor needs to be rebuilt. (For example, it may need to be updated with
   * current availabilities.)
   */
  public boolean needsRebuild() {
    return false;
  }

  /**
   * Rebuild this distributor.
   */
  public Distributor rebuild() {
    return new Distributor(xioClientConfig, nodes, strategy);
  }

  /**
   * Rebuild this distributor with a new vector.
   */
  public Distributor rebuild(Vector<Node> vector) {
    return new Distributor(xioClientConfig, vector, strategy);
  }

  private <T extends XioClientChannel> ListenableFuture<T> connectAsync(XioClientConnector clientChannelConnector, RetryPolicy retryPolicy) {

    return connectAsync(
        null,
        clientChannelConnector,
        retryPolicy,
        DEFAULT_CONNECT_TIMEOUT,
        DEFAULT_RECEIVE_TIMEOUT,
        DEFAULT_READ_TIMEOUT,
        DEFAULT_SEND_TIMEOUT,
        DEFAULT_MAX_FRAME_SIZE);
  }

  @SuppressWarnings("unchecked")
  private <T extends XioClientChannel> ListenableFuture<T> connectAsync(@Nullable ChannelHandlerContext ctx, XioClientConnector clientChannelConnector,  RetryPolicy retryPolicy) {

    return connectAsync(
        ctx,
        clientChannelConnector,
        retryPolicy,
        DEFAULT_CONNECT_TIMEOUT,
        DEFAULT_RECEIVE_TIMEOUT,
        DEFAULT_READ_TIMEOUT,
        DEFAULT_SEND_TIMEOUT,
        DEFAULT_MAX_FRAME_SIZE);
  }

  //  @SuppressWarnings("unchecked")
  private <T extends XioClientChannel> ListenableFuture<T> connectAsync(
      ChannelHandlerContext ctx,
      XioClientConnector clientChannelConnector,
      RetryPolicy retryPolicy,
      @Nullable Duration connectTimeout,
      @Nullable Duration receiveTimeout,
      @Nullable Duration readTimeout,
      @Nullable Duration sendTimeout,
      int maxFrameSize) {
    checkNotNull(clientChannelConnector, "clientChannelConnector is null");

    int retryCount = 0;
    long elapsedTime = 0;
    final Bootstrap bootstrap = new Bootstrap();
    final RetrySleeper sleeper = (time, unit) -> {
      Thread.sleep(time);
    };

    final long connectStart = System.currentTimeMillis();

    if (ctx != null) {
      bootstrap.group(ctx.channel().eventLoop().parent());
    } else {
      bootstrap.group(group);
    }

    bootstrap
        .channel(NioSocketChannel.class)
        .handler(clientChannelConnector.newChannelPipelineFactory(maxFrameSize, xioClientConfig));

    xioClientConfig.getBootstrapOptions().entrySet().forEach(xs -> {
      bootstrap.option(xs.getKey(), xs.getValue());
    });

    if (connectTimeout != null) {
      bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) connectTimeout.toMillis());
    }

    // Set some sane defaults
    bootstrap
        .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
        .option(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK, 32 * 1024)
        .option(ChannelOption.WRITE_BUFFER_LOW_WATER_MARK, 8 * 1024)
        .option(ChannelOption.TCP_NODELAY, true);

    ChannelFuture nettyChannelFuture = connect(clientChannelConnector, bootstrap, retryPolicy, retryCount, sleeper);

    return new XioFuture<>(clientChannelConnector,
        receiveTimeout,
        readTimeout,
        sendTimeout,
        nettyChannelFuture,
        xioClientConfig);
  }

  private static long getElapsedTime(long startTime) {
    return startTime - System.currentTimeMillis();
  }

  private ChannelFuture connect(XioClientConnector clientChannelConnector, Bootstrap bootstrap, RetryPolicy retryPolicy, int retryCount, RetrySleeper sleeper) {
    final long connectStart = System.currentTimeMillis();
    ChannelFuture[] channelFuture = new ChannelFuture[1];
    channelFuture[0] = clientChannelConnector.connect(bootstrap);
    channelFuture[0].addListener(new ChannelFutureListener() {
      @Override
      public void operationComplete(ChannelFuture future) throws Exception {
        Channel channel = future.channel();
        if (channel != null && channel.isOpen()) {
          allChannels.add(channel);
        } else {
          if (retryPolicy.allowRetry(retryCount, getElapsedTime(connectStart), sleeper)) {
            int newCount = retryCount + 1;
            channelFuture[0] = connect(clientChannelConnector, bootstrap, retryPolicy, newCount, sleeper);
          } else {
            log.error("Retry Count Exceeded - Failed to connect to server");
          }
        }
      }
    });
    return channelFuture[0];
  }

  @Override
  public void close() {
    // Stop the timer thread first, so no timeouts can fire during the rest of the
    // shutdown process
    timer.stop();

    ShutdownUtil.shutdownChannelFactory(
        group,
        null,
        null,
        allChannels);
  }

  //  protected type Distributor <: DistributorT { type This = Distributor }
//
//  /**
//   * Create an initial distributor.
//   */
//  protected def initDistributor(): Distributor
//
//  /**
//   * Balancer status is the best of its constituent nodes.
//   */
//  override def status: Status = Status.bestOf(dist.vector, nodeStatus)
//
//  private[this] val nodeStatus: Node => Status = _.factory.status
//
//  @volatile protected var dist: Distributor = initDistributor()
//
//  protected def rebuild(): Unit = {
//    updater(Rebuild(dist))
//  }
//
//  private[this] val gauges = Seq(
//      statsReceiver.addGauge("available") {
//    dist.vector.count(n => n.status == Status.Open)
//  },
//      statsReceiver.addGauge("busy") {
//    dist.vector.count(n => n.status == Status.Busy)
//  },
//      statsReceiver.addGauge("closed") {
//    dist.vector.count(n => n.status == Status.Closed)
//  },
//      statsReceiver.addGauge("load") {
//    dist.vector.map(_.pending).sum
//  },
//      statsReceiver.addGauge("size") { dist.vector.size })
//
//  private[this] val adds = statsReceiver.counter("adds")
//  private[this] val removes = statsReceiver.counter("removes")
//
//  protected sealed trait Update
//  protected case class NewList(
//  svcFactories: Traversable[ServiceFactory[Req, Rep]]) extends Update
//  protected case class Rebuild(cur: Distributor) extends Update
//  protected case class Invoke(fn: Distributor => Unit) extends Update
//
//  private[this] val updater = new Updater[Update] {
//    protected def preprocess(updates: Seq[Update]): Seq[Update] = {
//    if (updates.size == 1)
//      return updates
//
//    val types = updates.reverse.groupBy(_.getClass)
//
//    val update: Seq[Update] = types.get(classOf[NewList]) match {
//      case Some(Seq(last, _*)) => Seq(last)
//      case None => types.getOrElse(classOf[Rebuild], Nil).take(1)
//    }
//
//    update ++ types.getOrElse(classOf[Invoke], Nil).reverse
//    }
//
//    def handle(u: Update): Unit = u match {
//      case NewList(svcFactories) =>
//        val newFactories = svcFactories.toSet
//        val (transfer, closed) = dist.vector.partition { node =>
//        newFactories.contains(node.factory)
//      }
//
//      for (node <- closed)
//        node.close()
//      removes.incr(closed.size)
//
//      // we could demand that 'n' proxies hashCode, equals (i.e. is a Proxy)
//      val transferNodes = transfer.map(n => n.factory -> n).toMap
//      var numNew = 0
//      val newNodes = svcFactories.map {
//        case f if transferNodes.contains(f) => transferNodes(f)
//        case f =>
//          numNew += 1
//          newNode(f, statsReceiver.scope(f.toString))
//      }
//
//      dist = dist.rebuild(newNodes.toVector)
//      adds.incr(numNew)
//
//      case Rebuild(_dist) if _dist == dist =>
//        dist = dist.rebuild()
//
//      case Rebuild(_stale) =>
//
//      case Invoke(fn) =>
//        fn(dist)
//    }
//  }
//
//  /**
//   * Update the load balancer's service list. After the update, which
//   * may run asynchronously, is completed, the load balancer balances
//   * across these factories and no others.
//   */
//  def update(factories: Traversable[ServiceFactory[Req, Rep]]): Unit =
//  updater(NewList(factories))
//
//  /**
//   * Invoke `fn` on the current distributor. This is done through the updater
//   * and is serialized with distributor updates and other invocations.
//   */
//  protected def invoke(fn: Distributor => Unit): Unit = {
//    updater(Invoke(fn))
//  }
//
//  @tailrec
//  private[this] def pick(nodes: Distributor, count: Int): Node = {
//    if (count == 0)
//      return null.asInstanceOf[Node]
//
//    val n = dist.pick()
//    if (n.factory.status == Status.Open) n
//    else pick(nodes, count-1)
//  }
//
//  def apply(conn: ClientConnection): Future[Service[Req, Rep]] = {
//    val d = dist
//
//    var n = pick(d, maxEffort)
//    if (n == null) {
//      rebuild()
//      n = dist.pick()
//    }
//
//    val f = n(conn)
//    if (d.needsRebuild && d == dist)
//      rebuild()
//    f
//  }
//
//  def close(deadline: Time): Future[Unit] = {
//    for (gauge <- gauges) gauge.remove()
//    removes.incr(dist.vector.size)
//    Closable.all(dist.vector:_*).close(deadline)
//  }
//}
//
///**
// * A Balancer mix-in to provide automatic updating via Activities.
// */
//private trait Updating[Req, Rep] extends Balancer[Req, Rep] with OnReady {
//private[this] val ready = new Promise[Unit]
//    def onReady: Future[Unit] = ready
//
///**
// * An activity representing the active set of ServiceFactories.
// */
//protected def activity: Activity[Traversable[ServiceFactory[Req, Rep]]]
//
//  /*
//   * Subscribe to the Activity and dynamically update the load
//   * balancer as it (succesfully) changes.
//   *
//   * The observation is terminated when the Balancer is closed.
//   */
//private[this] val observation = activity.states.respond {
//    case Activity.Pending =>
//
//    case Activity.Ok(newList) =>
//    update(newList)
//    ready.setDone()
//
//    case Activity.Failed(_) =>
//    // On resolution failure, consider the
//    // load balancer ready (to serve errors).
//    ready.setDone()
//    }
//
//    override def close(deadline: Time): Future[Unit] = {
//    observation.close(deadline) transform { _ => super.close(deadline) } ensure {
//    ready.setDone()
//    }
//    }
//    }
//
///**
// * Provide Nodes whose 'load' is the current number of pending
// * requests and thus will result in least-loaded load balancer.
// */
//private trait LeastLoaded[Req, Rep] { self: Balancer[Req, Rep] =>
//protected def rng: RNG

}
