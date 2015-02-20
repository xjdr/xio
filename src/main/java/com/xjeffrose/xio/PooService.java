package com.xjeffrose.xio;

import java.nio.*;
import java.nio.charset.*;
import java.util.logging.*;

import com.xjeffrose.log.*;

class PooService extends Service {
  private static final Logger log = Log.getLogger(PooService.class.getName());

  PooService() {
  }

  public void handleGet() {
    resp.ok();
    resp.body("Hello from /poo \n");
  }

  public void handlePost() {
    resp.ok();
    resp.body(req.body.toString() + "\n"); //TODO: Should accept ByteBuffers for Proxy Functionality
  }

}
