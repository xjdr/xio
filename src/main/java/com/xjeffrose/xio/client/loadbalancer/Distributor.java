package com.xjeffrose.xio.client.loadbalancer;

import com.google.common.collect.ImmutableList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Vector;
import org.apache.log4j.Logger;

import static com.google.common.base.Preconditions.checkState;

/**
 * Creates a new Distributor to perform load balancing
 */
public class Distributor {
  private static final Logger log = Logger.getLogger(Distributor.class);

  private final ImmutableList<Node> pool;
  private final Map<UUID, Node> revLookup = new HashMap<>();
  private Strategy strategy;

  public Distributor(ImmutableList<Node> pool, Strategy strategy) {
    this.pool = pool;
    this.strategy = strategy;

    for (Node node : pool) {
      if (node.isAvailable()) {
        revLookup.put(node.token(), node);
      } else {
        log.error("Node is unreachable: " + node );
        pool.remove(node);
      }
    }

    checkState(pool.size() > 0, "Must be at least one reachable node in the pool");

  }

  /**
   * The vector of pool over which we are currently balancing.
   */
  public ImmutableList<Node> pool() {
    return pool;
  }

  /**
   * The node returned by UUID.
   */
  public Node getNodeById(UUID id) {
    return revLookup.get(id);
  }

  /**
   * Pick the next node. This is the main load balancer.
   */
  public Node pick() {
    return strategy.getNextNode(pool);
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

}
