
package com.xjeffrose.xio;

import java.io.*;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;

import com.xjeffrose.log.*;

class HelloService extends Service {
  private static final Logger log = Log.getLogger(HelloService.class.getName());

  HelloService() {
  }

  public void handleGet() {
    resp.ok();

  }

}
