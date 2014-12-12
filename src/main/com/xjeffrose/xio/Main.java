package com.xjeffrose.xio;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;

class Main {

  private static final Logger log = com.xjeffrose.log.Log.create();

  public static void main(String[] args) {
    log.info("Starting xio: Be well John Spartan");
    try {
      List<Filter> filters = new ArrayList<Filter>();
      GateKeeper g = new GateKeeper(8080, filters);
      EventLoop el = new EventLoop();

      el.register(g);
      el.run();
    } catch (IOException e) {
        e.printStackTrace();
    }

  }

}
