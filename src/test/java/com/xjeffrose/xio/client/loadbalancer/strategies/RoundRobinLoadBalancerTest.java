package com.xjeffrose.xio.client.loadbalancer.strategies;

import com.google.common.collect.ImmutableList;
import com.xjeffrose.xio.client.loadbalancer.Distributor;
import com.xjeffrose.xio.client.loadbalancer.Node;
import com.xjeffrose.xio.client.loadbalancer.Strategy;
import com.xjeffrose.xio.fixtures.TcpServer;
import java.net.InetSocketAddress;
import org.junit.Test;

import static org.junit.Assert.*;

public class RoundRobinLoadBalancerTest {
  TcpServer tcpServer1 = new TcpServer(8081);
  TcpServer tcpServer2 = new TcpServer(8082);
  TcpServer tcpServer3 = new TcpServer(8083);


  @Test
  public void getNextNodeUnweighted() throws Exception {
    new Thread(tcpServer1).start();
    new Thread(tcpServer2).start();
    new Thread(tcpServer3).start();

    Node node1 = new Node(new InetSocketAddress("127.0.0.1", 8081));
    Node node2 = new Node(new InetSocketAddress("127.0.0.1", 8082));
    Node node3 = new Node(new InetSocketAddress("127.0.0.1", 8083));

    Strategy lb = new RoundRobinLoadBalancer();
    Distributor distributor = new Distributor(ImmutableList.of(node1, node2, node3), lb);

    assertEquals(node1.address().getPort(), distributor.pick().address().getPort());
    assertEquals(node2.address().getPort(), distributor.pick().address().getPort());
    assertEquals(node3.address().getPort(), distributor.pick().address().getPort());
    assertEquals(node1.address().getPort(), distributor.pick().address().getPort());

  }

  @Test
  public void getNextNodeWeighted() throws Exception {
    new Thread(tcpServer1).start();
    new Thread(tcpServer2).start();
    new Thread(tcpServer3).start();

    Node node1 = new Node(new InetSocketAddress("127.0.0.1", 8081), 10);
    Node node2 = new Node(new InetSocketAddress("127.0.0.1", 8082), 100);
    Node node3 = new Node(new InetSocketAddress("127.0.0.1", 8083), 1);

    Strategy lb = new RoundRobinLoadBalancer();
    Distributor distributor = new Distributor(ImmutableList.of(node1, node2, node3), lb);

    assertEquals(node2.address().getPort(), distributor.pick().address().getPort());
    assertEquals(node1.address().getPort(), distributor.pick().address().getPort());
    assertEquals(node3.address().getPort(), distributor.pick().address().getPort());
    assertEquals(node2.address().getPort(), distributor.pick().address().getPort());

  }

  @Test()
  public void getEjectUnavailableNode() throws Exception {
    new Thread(tcpServer1).start();
//    new Thread(tcpServer2).start();
    new Thread(tcpServer3).start();

    Node node1 = new Node(new InetSocketAddress("127.0.0.1", 8081));
    Node node2 = new Node(new InetSocketAddress("127.0.0.1", 8082));
    Node node3 = new Node(new InetSocketAddress("127.0.0.1", 8083));

    Strategy lb = new RoundRobinLoadBalancer();
    Distributor distributor = new Distributor(ImmutableList.of(node1, node2, node3), lb);

//    assertEquals(node2.address().getPort(), distributor.pick().address().getPort());
    assertEquals(node1.address().getPort(), distributor.pick().address().getPort());
    assertEquals(node3.address().getPort(), distributor.pick().address().getPort());
    assertEquals(node1.address().getPort(), distributor.pick().address().getPort());

  }

}