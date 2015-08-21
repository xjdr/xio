package com.xjeffrose.xio.core;

public class RequestContexts
{
  private static ThreadLocal<RequestContext> threadLocalContext = new ThreadLocal<>();

  private RequestContexts()
  {
  }

  /**
   * Gets the thread-local {@link NiftyRequestContext} for the Thrift request that is being processed
   * on the current thread.
   *
   * @return The {@link NiftyRequestContext} of the current request
   */
  public static RequestContext getCurrentContext()
  {
    RequestContext currentContext = threadLocalContext.get();
    return currentContext;
  }

  /**
   * Sets the thread-local context for the currently running request.
   *
   * This is normally called only by the server, but it can also be useful to call when
   * dispatching to another thread (e.g. a thread in an ExecutorService) if the code that will
   * run on that thread might also be interested in the {@link RequestContext}
   */
  public static void setCurrentContext(RequestContext requestContext)
  {
    threadLocalContext.set(requestContext);
  }

  /**
   * Gets the thread-local context for the currently running request
   *
   * This is normally called only by the server, but it can also be useful to call when
   * cleaning up a context
   */
  public static void clearCurrentContext()
  {
    threadLocalContext.remove();
  }
}