
package com.xjeffrose.xio;

import java.io.*;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;

import com.xjeffrose.log.*;

class HelloService implements Service {
  private static final Logger log = Log.getLogger(HelloService.class.getName());

  private HttpRequest req;
  private HttpResponse resp;

  HelloService() {
  }

  public void handle(HttpRequest req, HttpResponse resp) {
    this.req = req;
    this.resp = resp;

    doHello();
  }

  private void doHello() {
    resp.ok();
    resp.body("Hello from / \n");
  }


}
