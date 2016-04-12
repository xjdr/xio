package com.xjeffrose.xio.client.loadbalancer.strategies;

import com.google.common.collect.ImmutableList;
import com.xjeffrose.xio.client.loadbalancer.Distributor;
import com.xjeffrose.xio.client.loadbalancer.Filter;
import com.xjeffrose.xio.client.loadbalancer.Node;
import com.xjeffrose.xio.client.loadbalancer.Strategy;
import com.xjeffrose.xio.fixtures.TcpServer;
import java.net.InetSocketAddress;
import org.junit.Test;

import static org.junit.Assert.*;

public class FilteredRoundRobinLoadBalancerTest {

  TcpServer tcpServer1 = new TcpServer(8081);
  TcpServer tcpServer2 = new TcpServer(8082);
  TcpServer tcpServer3 = new TcpServer(8083);

  ImmutableList<String> nokFilter = ImmutableList.of("thing1");
  ImmutableList<String> okFilter = ImmutableList.of("thing1", "thing2");
  ImmutableList<String> badFilter = ImmutableList.of("noThings");

  Node node1 = new Node(new InetSocketAddress("127.0.0.1", 8081));
  Node node2 = new Node(new InetSocketAddress("127.0.0.1", 8082));
  Node node3 = new Node(new InetSocketAddress("127.0.0.1", 8083));

  @Test
  public void getNextNode() throws Exception {
    new Thread(tcpServer1).start();
    new Thread(tcpServer2).start();
    new Thread(tcpServer3).start();

    Strategy lb = new FilteredRoundRobinLoadBalancer(new Filter() {

      @Override
      public boolean contains(String serviceName, String hostname, String item) {
        return true;
      }
    });
    Distributor distributor = new Distributor(ImmutableList.of(node1, node2, node3), lb);

    assertEquals(node2.address().getHostName(), distributor.pick().address().getHostName());
    assertEquals(node2.address().getHostName(), distributor.pick().address().getHostName());
    assertEquals(node2.address().getHostName(), distributor.pick().address().getHostName());
    assertEquals(node2.address().getHostName(), distributor.pick().address().getHostName());

  }

}