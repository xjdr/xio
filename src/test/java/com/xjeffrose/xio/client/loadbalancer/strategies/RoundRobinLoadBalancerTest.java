package com.xjeffrose.xio.client.loadbalancer.strategies;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.xjeffrose.xio.client.loadbalancer.Distributor;
import com.xjeffrose.xio.client.loadbalancer.Filter;
import com.xjeffrose.xio.client.loadbalancer.Node;
import com.xjeffrose.xio.client.loadbalancer.Strategy;
import com.xjeffrose.xio.fixtures.TcpServer;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.UUID;
import org.junit.Test;

import static org.junit.Assert.*;

public class RoundRobinLoadBalancerTest {
  TcpServer tcpServer1 = new TcpServer(8281);
  TcpServer tcpServer2 = new TcpServer(8282);
  TcpServer tcpServer3 = new TcpServer(8283);

  TcpServer tcpServer11 = new TcpServer(8381);
  TcpServer tcpServer12 = new TcpServer(8382);
  TcpServer tcpServer13 = new TcpServer(8383);

  @Test
  public void getNextNodeUnweighted() throws Exception {
    new Thread(tcpServer1).start();
    new Thread(tcpServer2).start();
    new Thread(tcpServer3).start();

    Node node1 = new Node(new InetSocketAddress("127.0.0.1", 8281));
    Node node2 = new Node(new InetSocketAddress("127.0.0.1", 8282));
    Node node3 = new Node(new InetSocketAddress("127.0.0.1", 8283));

    Strategy lb = new RoundRobinLoadBalancer();
    Distributor distributor = new Distributor(ImmutableList.of(node1, node2, node3), lb);

    assertEquals(node1.address().getPort(), distributor.pick().address().getPort());
    assertEquals(node2.address().getPort(), distributor.pick().address().getPort());
    assertEquals(node3.address().getPort(), distributor.pick().address().getPort());
    assertEquals(node1.address().getPort(), distributor.pick().address().getPort());

  }

  @Test
  public void getNextNodeWeighted() throws Exception {
    Node node1 = new Node(new InetSocketAddress("127.0.0.1", 8281), 10);
    Node node2 = new Node(new InetSocketAddress("127.0.0.1", 8282), 100);
    Node node3 = new Node(new InetSocketAddress("127.0.0.1", 8283), 1);

    Strategy lb = new RoundRobinLoadBalancer();
    Distributor distributor = new Distributor(ImmutableList.of(node1, node2, node3), lb);

    assertEquals(node2.address().getPort(), distributor.pick().address().getPort());
    assertEquals(node1.address().getPort(), distributor.pick().address().getPort());
    assertEquals(node3.address().getPort(), distributor.pick().address().getPort());
    assertEquals(node2.address().getPort(), distributor.pick().address().getPort());

  }

  @Test()
  public void getEjectUnavailableNode() throws Exception {
    new Thread(tcpServer11).start();
    new Thread(tcpServer13).start();

    Node node1 = new Node(new InetSocketAddress("127.0.0.1", 8381));
    Node node2 = new Node(new InetSocketAddress("127.0.0.1", 8382));
    Node node3 = new Node(new InetSocketAddress("127.0.0.1", 8383));

    Strategy lb = new RoundRobinLoadBalancer();
    Distributor distributor = new Distributor(ImmutableList.of(node1, node2, node3), lb);

    // Sleep is required to allow for node refresh
    Thread.sleep(5500);

    assertEquals(node1.address().getPort(), distributor.pick().address().getPort());
    assertEquals(node3.address().getPort(), distributor.pick().address().getPort());
    assertEquals(node1.address().getPort(), distributor.pick().address().getPort());
  }

  @Test(expected = NullPointerException.class)
  public void preventStackOverflow() throws Exception {
    Node node1 = new Node(new InetSocketAddress("127.0.0.1", 8481));
    Node node2 = new Node(new InetSocketAddress("127.0.0.1", 8482));
    Node node3 = new Node(new InetSocketAddress("127.0.0.1", 8483));

    Strategy lb = new RoundRobinLoadBalancer();
    Distributor distributor = new Distributor(ImmutableList.of(node1, node2, node3), lb);

    // Sleep is required to allow for node refresh to eject node
    Thread.sleep(5500);

    assertEquals(node1.address().getPort(), distributor.pick().address().getPort());
    assertEquals(node2.address().getPort(), distributor.pick().address().getPort());
    assertEquals(node3.address().getPort(), distributor.pick().address().getPort());
    assertEquals(node1.address().getPort(), distributor.pick().address().getPort());
  }

  @Test
  public void testRoundrobinAndNoOverflow() throws Exception {

    Node node1 = new Node(new InetSocketAddress("127.0.0.1", 8181));
    Node node2 = new Node(new InetSocketAddress("127.0.0.2", 8182));
    Node node3 = new Node(new InetSocketAddress("127.0.0.3", 8183));

    Strategy lb = new RoundRobinLoadBalancer();
    ImmutableList<Node> pool = ImmutableList.of(node1, node2, node3);

    assertEquals(node1, lb.getNextNode(pool, ImmutableMap.of(node1.token(), node1)));
    assertEquals(node2, lb.getNextNode(pool, ImmutableMap.of(node2.token(), node2)));
    assertEquals(node3, lb.getNextNode(pool, ImmutableMap.of(node3.token(), node3)));

    // test restarting from idx 0 to make sure no overflow
    assertEquals(node1, lb.getNextNode(pool, ImmutableMap.of(node1.token(), node1)));
    assertEquals(node2, lb.getNextNode(pool, ImmutableMap.of(node2.token(), node2)));
    assertEquals(node3, lb.getNextNode(pool, ImmutableMap.of(node3.token(), node3)));

  }
}
