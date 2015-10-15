package com.xjeffrose.xio.client.loadbalancer;

public class Node implements NodeT {

  public Node() {

  }

  public Node newNode() {
    return new Node();
  }

  protected Node failingNode() {

    return new Node();
  }

  @Override
  public double load() {
    return 0;
  }

  @Override
  public int pending() {
    return 0;
  }

  @Override
  public int token() {
    return 0;
  }

  //    protected case class Node(factory: ServiceFactory[Req, Rep], metric: Metric, token: Int)
//    extends ServiceFactoryProxy[Req, Rep](factory)
//        with NodeT {
//      type This = Node
//
//      def load = metric.get()
//      def pending = metric.rate()
//
//      override def apply(conn: ClientConnection) = {
//        val ts = metric.start()
//        super.apply(conn) transform {
//          case Return(svc) =>
//            Future.value(new ServiceProxy(svc) {
//              override def close(deadline: Time) =
//                  super.close(deadline) ensure {
//                metric.end(ts)
//              }
//            })
//
//          case t@Throw(_) =>
//            metric.end(ts)
//            Future.const(t)
//        }
//      }
//    }
//
//  protected def newNode(factory: ServiceFactory[Req, Rep], statsReceiver: StatsReceiver): Node =
//  Node(factory, new Metric(statsReceiver, factory.toString), rng.nextInt())
//
//  protected def failingNode(cause: Throwable) = Node(
//      new FailingFactory(cause),
//  new Metric(NullStatsReceiver, "failing"),
//  0
//      )
//}

}
