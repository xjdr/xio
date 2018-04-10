========
 Client
========

The :java:extdoc:`Client<com.xjeffrose.xio.client.XioClient>`
object holds the state and configuration data for a single abstract
client.  Depending on the concrete implementation the client could be
connected to multiple servers in a cluster, or just a single server.
It should be configured and instantiated by a
:java:extdoc:`XioClientBootstrap<com.xjeffrose.xio.client.XioClientBootstrap>`
object.

.. code-block:: java
   :linenos:

   XioClient client = new XioClientBootstrap(new NioEventLoopGroup())
     .setAddress(new InetSocketAddress("10.10.10.10", 443))
     .handler(new SimpleInboundChannelHandler())
     .build();

The handler defines how the client will interact with the remote
server. By default clients will use HTTP as their application
protocol.
