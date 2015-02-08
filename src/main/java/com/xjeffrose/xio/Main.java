package com.xjeffrose.xio;

import java.io.*;
import java.util.concurrent.*;
import java.util.concurrent.Future;
import java.util.logging.*;

import com.xjeffrose.log.*;

class Main {
  private static final Logger log = Log.getLogger(Main.class.getName());

  public static void main(String[] args) {
    log.info("Starting xio: Be well John Spartan");

    try {
      Service rootService = new HelloService();
      rootService.andThen(new MelloService());

      Server s = new Server();
      s.addRoute("/", rootService);
      s.addRoute("/poo", new PooService());

      Future<Server> sf = s.serve(8080);

      /* Thread.sleep(500); */
      /*  */
      /* sf.get().close(); */
      /* System.exit(0); */

      /* Client c = new Client(); */
      /* c.defaultRequest(); */
      /* try { */
      /*   Thread.sleep(200); */
      /*   Future<HttpResponse> f = c.get(8000); */
      /*   HttpResponse resp = f.get(); */
      /*   log.info(resp.uri.get()); */
      /* } catch (Exception e) { */
      /*   throw new RuntimeException(e); */
      /* } */
      /* s.serve(8080, 8); */
      /* s.serve("localhost",8080); */
      /* s.serve("localhost",8080, 8); */
      /* s.serve(127.0.0.1,8080); */
      /* serve(127.0.0.1,8080,8); */

      // Await.ready(s.serve()); => all async and stuff

    } catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }

  }
}
