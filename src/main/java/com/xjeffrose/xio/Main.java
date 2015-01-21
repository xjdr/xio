package com.xjeffrose.xio;

import java.io.*;
import java.util.logging.*;

import com.xjeffrose.log.*;

class Main {
  private static final Logger log = Log.getLogger(Main.class.getName());

  public static void main(String[] args) {
    log.info("Starting xio: Be well John Spartan");

    try {
      Server s = new Server();
      Client c = new Client();
      // s.addroute("/", exampleService)

      s.serve(8080);
      /* c.get(8080); */
      /* s.serve(8080, 8); */
      /* s.serve("localhost",8080); */
      /* s.serve("localhost",8080, 8); */
      /* s.serve(127.0.0.1,8080); */
      /* serve(127.0.0.1,8080,8); */

      // Await.ready(s.serve()); => all async and stuff

    } catch (IOException e) {
      e.printStackTrace();
      System.exit(-1);
    }

  }
}
