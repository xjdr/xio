package com.xjeffrose.xio;

class SimpleService implements Service {

  public HttpRequest req;
  public HttpResponse resp;

  SimpleService() {
  }

  public void handle(HttpRequest req, HttpResponse resp) {
    this.req = req;
    this.resp = resp;

    switch(req.method_) {
      case get:
        handleGet();
        return;
      case post:
        handlePost();
        return;
      case put:
        handlePut();
        return;
      case delete:
        handlePost();
        return;
      default:
        return;
    }
  }

  public void handleGet() {}

  public void handlePost() {}

  public void handlePut() {}

  public void handleDelete() {}


}
