package com.xjeffrose.xio.fixtures;

import java.io.IOException;
import java.net.InetSocketAddress;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;

public class SimpleTestServer extends AbstractHandler implements Runnable, AutoCloseable {

  private int port;
  private int boundPort;
  private Server server;

  public SimpleTestServer(int port) {
    this.port = port;
  }

  public void handle(String target,
                     Request baseRequest,
                     HttpServletRequest request,
                     HttpServletResponse response)
      throws IOException, ServletException
  {
    response.setContentType("text/html;charset=utf-8");
    response.setStatus(HttpServletResponse.SC_OK);
    response.setHeader("X-TEST-HEADER", request.getHeader("X-TEST-HEADER"));
    baseRequest.setHandled(true);
    response.getWriter().println("CONGRATS!");
  }

  public InetSocketAddress boundAddress() {
    return new InetSocketAddress("127.0.0.1", boundPort);
  }

  public void close() {
    try {
      server.stop();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void run() {
    server = new Server(port);
    server.setHandler(this);

    try {
      server.start();
      boundPort = ((ServerConnector)server.getConnectors()[0]).getLocalPort();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
