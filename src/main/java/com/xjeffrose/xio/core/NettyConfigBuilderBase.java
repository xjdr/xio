package com.xjeffrose.xio.core;

import com.google.common.base.Preconditions;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import org.jboss.netty.util.Timer;

/*
 * Hooks for configuring various parts of Netty.
 */
public abstract class NettyConfigBuilderBase<T extends NettyConfigBuilderBase<T>> {
  // These constants come directly from Netty but are private in Netty.
  public static final int DEFAULT_BOSS_THREAD_COUNT = 1;
  public static final int DEFAULT_WORKER_THREAD_COUNT = Runtime.getRuntime().availableProcessors() * 2;

  private final Map<String, Object> options = new HashMap<>();
  private String XioName;
  private int bossThreadCount = DEFAULT_BOSS_THREAD_COUNT;
  private int workerThreadCount = DEFAULT_WORKER_THREAD_COUNT;
  private ExecutorService bossThreadExecutor;
  private ExecutorService workerThreadExecutor;
  private Timer timer;

  public Map<String, Object> getBootstrapOptions() {
    return Collections.unmodifiableMap(options);
  }

  protected Timer getTimer() {
    return timer;
  }

  public T setTimer(Timer timer) {
    this.timer = timer;
    return (T) this;
  }

  public String getXioName() {
    return XioName;
  }

  protected T setXioName(String XioName) {
    Preconditions.checkNotNull(XioName, "XioName cannot be null");
    this.XioName = XioName;
    return (T) this;
  }

  public T setBossThreadExecutor(ExecutorService bossThreadExecutor) {
    this.bossThreadExecutor = bossThreadExecutor;
    return (T) this;
  }

  protected ExecutorService getBossExecutor() {
    return bossThreadExecutor;
  }

  protected int getBossThreadCount() {
    return bossThreadCount;
  }

  public T setBossThreadCount(int bossThreadCount) {
    this.bossThreadCount = bossThreadCount;
    return (T) this;
  }

  public T setWorkerThreadExecutor(ExecutorService workerThreadExecutor) {
    this.workerThreadExecutor = workerThreadExecutor;
    return (T) this;
  }

  protected ExecutorService getWorkerExecutor() {
    return workerThreadExecutor;
  }

  protected int getWorkerThreadCount() {
    return workerThreadCount;
  }

  public T setWorkerThreadCount(int workerThreadCount) {
    this.workerThreadCount = workerThreadCount;
    return (T) this;
  }

  // Magic alert ! Content of this class is considered ugly and magical.
  // For all intents and purposes this is to create a Map with the correct
  // key and value pairs for Netty's Bootstrap to consume.
  //
  // sadly Netty does not define any constant strings whatsoever for the proper key to
  // use and it's all based on standard java bean attributes.
  //
  // A ChannelConfig impl in netty is also tied with a socket, but since all
  // these configs are interfaces we can do a bit of magic hacking here.

  protected class Magic implements InvocationHandler {
    private final String prefix;

    public Magic(String prefix) {
      this.prefix = prefix;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args)
        throws Throwable {
      // we are only interested in setters with single arg
      if (proxy != null) {
        if (method.getName().equals("toString")) {
          return "this is a magic proxy";
        } else if (method.getName().equals("equals")) {
          return Boolean.FALSE;
        } else if (method.getName().equals("hashCode")) {
          return 0;
        }
      }
      // we don't support multi-arg setters
      if (method.getName().startsWith("set") && args.length == 1) {
        String attributeName = method.getName().substring(3);
        // camelCase it
        attributeName = attributeName.substring(0, 1).toLowerCase() + attributeName.substring(1);
        // now this is our key
        options.put(prefix + attributeName, args[0]);
        return null;
      }
      throw new UnsupportedOperationException();
    }
  }
}
