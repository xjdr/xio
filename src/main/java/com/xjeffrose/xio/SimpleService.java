package com.xjeffrose.xio;

class SimpleService implements Service {

  public HttpRequest req;
  public HttpResponse resp;

  SimpleService() {
  }

  public void handle(HttpRequest req, HttpResponse resp) {
    this.req = req;
    this.resp = resp;

    doHandle();
  }

  public void doHandle() {
  }


}
