package com.xjeffrose.xio.client.loadbalancer;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.log4j.Logger;

import static com.google.common.base.Preconditions.checkState;

/**
 * Creates a new Distributor to perform load balancing
 */
public class Distributor {
  private static final Logger log = Logger.getLogger(Distributor.class);
  private final ImmutableList<Node> pool;
  private final Map<UUID, Node> okNodes = new ConcurrentHashMap<>();
  private final Strategy strategy;
  private final Timer t = new Timer();
  private final Ordering<Node> byWeight = Ordering.natural().onResultOf(
      new Function<Node, Integer>() {
        public Integer apply(Node node) {
          return node.getWeight();
        }
      }
  ).reverse();

  public Distributor(ImmutableList<Node> pool, Strategy strategy) {
    this.pool = ImmutableList.copyOf(byWeight.sortedCopy(pool));
    this.strategy = strategy;

    // assume all are reachable before the first health check
    for (Node node : pool) {
      okNodes.put(node.token(), node);
    }

    checkState(pool.size() > 0, "Must be at least one reachable node in the pool");

    t.schedule(new TimerTask() {

      @Override
      public void run() {
        refreshPool();
      }
    }, 5000, 5000);
  }

  private void refreshPool() {
    for (Node node : pool) {
      if (node.isAvailable()) {
        okNodes.putIfAbsent(node.token(), node);
      } else {
        log.error("Node is unreachable: " + node.address().getHostName() + ":" + node.address().getPort());
        okNodes.remove(node.token());
      }
    }
    checkState(okNodes.keySet().size() > 0, "Must be at least one reachable node in the pool");
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
    return new Distributor(pool, strategy);
  }

  /**
   * Rebuild this distributor with a new vector.
   */
  public Distributor rebuild(ImmutableList<Node> list) {
    return new Distributor(list, strategy);
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
            ns.setHealthy(okNodes.containsKey(node.token()));
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
