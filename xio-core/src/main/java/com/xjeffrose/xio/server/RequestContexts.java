package com.xjeffrose.xio.server;

public class RequestContexts {
  private static ThreadLocal<RequestContext> threadLocalContext = new ThreadLocal<>();

  private RequestContexts() {}

  public static RequestContext getCurrentContext() {
    RequestContext currentContext = threadLocalContext.get();
    return currentContext;
  }

  public static void setCurrentContext(RequestContext requestContext) {
    threadLocalContext.set(requestContext);
  }

  public static void clearCurrentContext() {
    threadLocalContext.remove();
  }
}
