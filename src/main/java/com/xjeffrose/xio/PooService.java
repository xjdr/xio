package com.xjeffrose.xio;

import java.util.logging.*;

import com.xjeffrose.log.*;

class PooService extends SimpleService {
  private static final Logger log = Log.getLogger(PooService.class.getName());

  PooService() {
  }

  @Override
  public void doHandle() {
    resp.ok();
    resp.body("Hello from /poo \n");
  }


}
