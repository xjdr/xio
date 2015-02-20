package com.xjeffrose.xio;

import java.util.concurrent.*;

public abstract class Service {

  public Route route;
  public HttpRequest req;
  public HttpResponse resp;

  private final ConcurrentLinkedDeque<Service> serviceList = new ConcurrentLinkedDeque<Service>();

  public Service() {
  }

  public void handle(Route route, HttpRequest req, HttpResponse resp) {
    this.route = route;
    this.req = req;
    this.resp = resp;

    switch(req.method_) {
      case get:
        handleGet();
        serviceStream();
        return;
      case post:
        handlePost();
        serviceStream();
        return;
      case put:
        handlePut();
        serviceStream();
        return;
      case delete:
        handleDelete();
        serviceStream();
        return;
      default:
        handleGet();
        serviceStream();
        return;
    }
  }

  public void handleGet() {}

  public void handlePost() {}

  public void handlePut() {}

  public void handleDelete() {}

  public void andThen(Service service) {
    serviceList.addLast(service);
  }

  private void serviceStream() {
    while (serviceList.size() > 0) {
      serviceList.removeLast().handle(route,req,resp);
    }
  }

}
