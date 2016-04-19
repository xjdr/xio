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

import static org.junit.Assert.assertEquals;

public class FilteredRoundRobinLoadBalancerTest {

  @Test
  public void getNextNode() throws Exception {
    TcpServer tcpServer1 = new TcpServer(8181);
    TcpServer tcpServer2 = new TcpServer(8182);
    TcpServer tcpServer3 = new TcpServer(8183);

    ImmutableList<String> nokFilter = ImmutableList.of("thing1");
    ImmutableList<String> okFilter = ImmutableList.of("thing1", "thing2");
    ImmutableList<String> badFilter = ImmutableList.of("noThings");

    Node node1 = new Node(new InetSocketAddress("127.0.0.1", 8181));
    Node node2 = new Node(new InetSocketAddress("127.0.0.1", 8182));
    Node node3 = new Node(new InetSocketAddress("127.0.0.1", 8183));

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

  @Test
  public void getNextNodeNoFiltering() throws Exception {

    Node node1 = new Node(new InetSocketAddress("127.0.0.1", 8181));
    Node node2 = new Node(new InetSocketAddress("127.0.0.2", 8182));
    Node node3 = new Node(new InetSocketAddress("127.0.0.3", 8183));

    Strategy lb = new FilteredRoundRobinLoadBalancer(new Filter() {

      @Override
      public boolean contains(String serviceName, String hostname, String item) {
        return true;
      }
    });
    ImmutableList<Node> pool = ImmutableList.of(node1, node2, node3);

    assertEquals(node1, lb.getNextNode(pool, ImmutableMap.of(node1.token(), node1)));
    assertEquals(node2, lb.getNextNode(pool, ImmutableMap.of(node2.token(), node2)));
    assertEquals(node3, lb.getNextNode(pool, ImmutableMap.of(node3.token(), node3)));

    // test restarting from idx 0 to make sure no overflow
    assertEquals(node1, lb.getNextNode(pool, ImmutableMap.of(node1.token(), node1)));
    assertEquals(node2, lb.getNextNode(pool, ImmutableMap.of(node2.token(), node2)));
    assertEquals(node3, lb.getNextNode(pool, ImmutableMap.of(node3.token(), node3)));

  }

  @Test
  public void getNextNodeWithFiltering() throws Exception {

    Node node1 = new Node(new InetSocketAddress("127.0.0.1", 8181),
        ImmutableList.copyOf(new String[]{"msmaster1int"}), 0, "paymentserv");
    Node node2 = new Node(new InetSocketAddress("127.0.0.2", 8182),
        ImmutableList.copyOf(new String[]{"msmaster2int"}), 0, "paymentserv");
    Node node3 = new Node(new InetSocketAddress("127.0.0.3", 8183),
        ImmutableList.copyOf(new String[]{"msmaster1int"}), 0, "paymentserv");

    Strategy lb = new FilteredRoundRobinLoadBalancer(new Filter() {

      @Override
      public boolean contains(String serviceName, String hostname, String item) {
        return "msmaster1int".equals(item);
      }
    });
    ImmutableList<Node> pool = ImmutableList.of(node1, node2, node3);

    assertEquals(node1, lb.getNextNode(pool, ImmutableMap.of(node1.token(), node1)));
    assertEquals(node3, lb.getNextNode(pool, ImmutableMap.of(node3.token(), node3)));

    assertEquals(node1, lb.getNextNode(pool, ImmutableMap.of(node1.token(), node1)));
    assertEquals(node3, lb.getNextNode(pool, ImmutableMap.of(node3.token(), node3)));
  }

}
