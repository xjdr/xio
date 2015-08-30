package com.xjeffrose.xio.fixtures;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

public class SimpleTestServer extends AbstractHandler implements Runnable {

  private int port;

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

  public void run() {
    {
      Server server = new Server(port);
      server.setHandler(new SimpleTestServer(port));

      try {
        server.start();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }
}