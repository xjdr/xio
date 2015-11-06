package com.xjeffrose.xio;

import java.net.InetSocketAddress;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * Listing 2.w  of <i>Netty in Action</i>
 *
 * @author <a href="mailto:norman.maurer@googlemail.com">Norman Maurer</a>
 */
public class EchoServer implements Runnable {

    private final int port;

    public EchoServer(int port) {

        this.port = port;
    }

    @Override
    public void run() {

        NioEventLoopGroup group = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(group)
                .channel(NioServerSocketChannel.class)
                .localAddress(new InetSocketAddress("127.0.0.1", port))
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch)
                        throws Exception {
                        ch.pipeline().addLast(
                            new com.xjeffrose.xio.EchoServerHandler());
                    }
                });

            ChannelFuture f = b.bind().sync();
            System.out.println(EchoServer.class.getName() + " started and listen on " + f.channel().localAddress());
            f.channel().closeFuture().sync();
        } catch(Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                group.shutdownGracefully().sync();
            } catch(Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}

