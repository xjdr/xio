package com.xjeffrose.xio.client.loadbalancer;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Ordering;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
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
  private ScheduledExecutorService nodeCheckExecutorService;
  private final String serviceName;
  private boolean healthCheckEnabled;

  private final Ordering<Node> byWeight = Ordering.natural().onResultOf(
      new Function<Node, Integer>() {
        public Integer apply(Node node) {
          return node.getWeight();
        }
      }
  ).reverse();

  public Distributor(ImmutableList<Node> pool, Strategy strategy, NodeHealthCheck nodeHealthCheck, String serviceName) {
     this(pool,strategy,nodeHealthCheck,serviceName,false);
  }

  public Distributor(ImmutableList<Node> pool, Strategy strategy, NodeHealthCheck nodeHealthCheck, String serviceName, boolean healthCheckEnabled) {
    this.nodeHealthCheck = nodeHealthCheck;
    this.pool = ImmutableList.copyOf(byWeight.sortedCopy(pool));
    this.strategy = strategy;
    this.okNodes = new ConcurrentHashMap<>();
    this.serviceName = serviceName;
    this.healthCheckEnabled = healthCheckEnabled;

    // assume all are reachable before the first health check
    for (Node node : pool) {
      okNodes.put(node.token(), node);
    }
    checkState(pool.size() > 0, "Must be at least one reachable node in the pool");
    if(healthCheckEnabled){
      nodeCheckExecutorService = Executors.newSingleThreadScheduledExecutor();
      registerHealthCheck();
    }
  }

  private Distributor(ImmutableList<Node> pool, Map<UUID, Node> okNodes, Strategy strategy, NodeHealthCheck nodeHealthCheck, String serviceName){
    this(pool,okNodes,strategy,nodeHealthCheck,serviceName,false);
  }

  private Distributor(ImmutableList<Node> pool, Map<UUID, Node> okNodes, Strategy strategy, NodeHealthCheck nodeHealthCheck, String serviceName, boolean healthCheckEnabled){
    this.nodeHealthCheck = nodeHealthCheck;
    this.pool = ImmutableList.copyOf(byWeight.sortedCopy(pool));
    this.strategy = strategy;
    this.okNodes = ImmutableMap.copyOf(okNodes);
    this.serviceName= serviceName;
    this.healthCheckEnabled = healthCheckEnabled;
    if(healthCheckEnabled){
      nodeCheckExecutorService = Executors.newSingleThreadScheduledExecutor();
      registerHealthCheck();
    }

  }

  private void registerHealthCheck(){
    Random r = new Random();
    //node health is scheduled at some random value between 5 and 35 sec.
    nodeCheckExecutorService.scheduleAtFixedRate(new Runnable(){
      @Override
      public void run() {
        Thread.currentThread().setName("HeatlhCheck-"+serviceName);
        refreshPool();
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
  }

  public void stop(){
    if(healthCheckEnabled) {
      this.nodeCheckExecutorService.shutdownNow();
      try {
        this.nodeCheckExecutorService.awaitTermination(200, TimeUnit.MILLISECONDS);
      } catch (Exception e) {
        log.error("Forcefully stopping Executor Service");
      }
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
    stop();
    return new Distributor(pool, strategy, nodeHealthCheck,serviceName, healthCheckEnabled);
  }

  public Distributor rebuild(ImmutableList<Node> nodes, Map<UUID,Node> okNodes){
    stop();
    return new Distributor(nodes, okNodes, this.strategy, this.nodeHealthCheck, serviceName, healthCheckEnabled);
  }

  /**
   * Rebuild this distributor with a new vector.
   */
  public Distributor rebuild(ImmutableList<Node> list) {
    stop();
    return new Distributor(list, strategy, nodeHealthCheck, serviceName, healthCheckEnabled);
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
