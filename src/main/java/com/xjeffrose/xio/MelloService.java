package com.xjeffrose.xio;

import java.io.*;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;

import com.xjeffrose.log.*;

class MelloService extends Service {
  private static final Logger log = Log.getLogger(MelloService.class.getName());

  MelloService() {
  }

  public void handleGet() {
    resp.ok();

    resp.body("From Mello");
  }

}
