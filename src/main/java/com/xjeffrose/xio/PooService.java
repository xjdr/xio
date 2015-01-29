package com.xjeffrose.xio;

import java.nio.*;
import java.nio.charset.*;
import java.util.logging.*;

import com.xjeffrose.log.*;

class PooService extends SimpleService {
  private static final Logger log = Log.getLogger(PooService.class.getName());

  PooService() {
  }

  public void handleGet() {
    resp.ok();
    resp.body("Hello from /poo \n");
  }

  public void handlePost() {
    resp.ok();
    byte[] reqBites = new byte[req.contentLength()];
    ByteBuffer temp = req.getBody();
    temp.get(reqBites);
    String bodyString = new String(reqBites, Charset.forName("UTF-8"));
    log.info(bodyString);
    resp.body(bodyString + "\n");
  }


}
