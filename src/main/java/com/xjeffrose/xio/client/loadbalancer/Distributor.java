package com.xjeffrose.xio.client.loadbalancer;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Ordering;
import com.xjeffrose.xio.client.retry.ExponentialBackoffRetry;
import com.xjeffrose.xio.core.XioTimer;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.log4j.Logger;

import static com.google.common.base.Preconditions.checkState;

/**
 * Creates a new Distributor to perform load balancing
 */
public class Distributor {
  private static final Logger log = Logger.getLogger(Distributor.class);
  private final ImmutableList<Node> pool;
  private final Map<UUID, Node> okNodes;
  private final Strategy strategy;
  private final NodeHealthCheck nodeHealthCheck;
  private final ScheduledExecutorService nodeCheck = Executors.newSingleThreadScheduledExecutor();
  //private final TimeUnit nodeCheckInterval;

  private final Ordering<Node> byWeight = Ordering.natural().onResultOf(
      new Function<Node, Integer>() {
        public Integer apply(Node node) {
          return node.getWeight();
        }
      }
  ).reverse();

  public Distributor(ImmutableList<Node> pool, Strategy strategy, NodeHealthCheck nodeHealthCheck) {
    this.nodeHealthCheck = nodeHealthCheck;
    this.pool = ImmutableList.copyOf(byWeight.sortedCopy(pool));
    this.strategy = strategy;
    this.okNodes = new ConcurrentHashMap<>();

    // assume all are reachable before the first health check
    for (Node node : pool) {
      okNodes.put(node.token(), node);
    }

    checkState(pool.size() > 0, "Must be at least one reachable node in the pool");
    registerHealthCheck();
  }

  private Distributor(ImmutableList<Node> pool, Map<UUID, Node> okNodes, Strategy strategy, NodeHealthCheck nodeHealthCheck){
    this.nodeHealthCheck = nodeHealthCheck;
    this.pool = ImmutableList.copyOf(byWeight.sortedCopy(pool));
    this.strategy = strategy;
    this.okNodes = ImmutableMap.copyOf(okNodes);
    registerHealthCheck();
  }

  private void registerHealthCheck(){
    Random r = new Random();
    //node health is scheduled at some random value between 5 and 35 sec.
    nodeCheck.scheduleAtFixedRate(new Runnable(){
      @Override
      public void run() {
        nodeHealthCheck.getExec().submit(() -> refreshPool());
      }
    },0,r.nextInt(31)+5,TimeUnit.SECONDS);
  }

  private void refreshPool() {
    for (Node node : pool) {
      try {
        nodeHealthCheck.connect(node, node.getProto(), node.isSSL(), null);
      }catch (Exception e){
        //What do we do here ??
      }

      if (node.isAvailable()) {
        okNodes.putIfAbsent(node.token(), node);
      } else {
        log.debug("Node is unreachable: " + node.address().getHostName() + ":" + node.address().getPort());
        okNodes.remove(node.token());
      }
    }
    checkState(okNodes.keySet().size() > 0, "Must be at least one reachable node in the pool");
    //Re-register the healthcheck
    //registerHealthCheck();
  }

  public void stop(){
    this.nodeCheck.shutdownNow();
    try {
      this.nodeCheck.awaitTermination(200, TimeUnit.MILLISECONDS);
    }catch (Exception e){
      log.error("Forcefully stopping Executor Service");
    }
  }

  /**
   * The vector of pool over which we are currently balancing.
   */
  private ImmutableList<Node> pool() {
    return pool;
  }

  /**
   * The node returned by UUID.
   */
  public Node getNodeById(UUID id) {
    return okNodes.get(id);
  }

  /**
   * Pick the next node. This is the main load balancer.
   */
  public Node pick() {
    return strategy.getNextNode(pool, okNodes);
  }

  /**
   * Rebuild this distributor.
   */
  public Distributor rebuild() {
    this.nodeCheck.shutdown();
    return new Distributor(pool, strategy, nodeHealthCheck);
  }

  public Distributor rebuild(ImmutableList<Node> nodes, Map<UUID,Node> okNodes){
    this.nodeCheck.shutdown();
    return new Distributor(nodes, okNodes, this.strategy, this.nodeHealthCheck);
  }

  /**
   * Rebuild this distributor with a new vector.
   */
  public Distributor rebuild(ImmutableList<Node> list) {
    this.nodeCheck.shutdown();
    return new Distributor(list, strategy, nodeHealthCheck);
  }

  public ImmutableList<Node> getPool() {
    return pool;
  }

  public List<NodeStat> getNodeStat() {
    ImmutableList<Node> nodes = ImmutableList.copyOf(this.pool());
    List<NodeStat> nodeStat = new ArrayList<>();
    if (nodes != null && !nodes.isEmpty()) {
      nodes.stream()
          .forEach(node -> {
            NodeStat ns = new NodeStat(node);
            ns.setHealthy(node.isAvailable());
            ns.setUsedForRouting(strategy.okToPick(node));
            nodeStat.add(ns);
          });
    }
    return nodeStat;
  }

  public Map<UUID, Node> getOkNodes() {
    return okNodes;
  }

}
