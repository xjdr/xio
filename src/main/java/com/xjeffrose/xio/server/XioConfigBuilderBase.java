package com.xjeffrose.xio.server;

import com.google.common.base.Preconditions;
import io.netty.channel.ChannelOption;
import io.netty.util.Timer;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

public abstract class XioConfigBuilderBase<T extends XioConfigBuilderBase<T>> {
  // These constants come directly from Netty but are private in Netty.
  private static final int DEFAULT_BOSS_THREAD_COUNT = 4;
  private static final int DEFAULT_WORKER_THREAD_COUNT = Runtime.getRuntime().availableProcessors() * 2;

  private Map<ChannelOption<Object>, Object> options = new HashMap<>();
  private String XioName;
  private int bossThreadCount = DEFAULT_BOSS_THREAD_COUNT;
  private int workerThreadCount = DEFAULT_WORKER_THREAD_COUNT;
  private ExecutorService bossThreadExecutor;
  private ExecutorService workerThreadExecutor;
  private Timer timer;

  public Map<ChannelOption<Object>, Object> getBootstrapOptions() {
    return Collections.unmodifiableMap(options);
  }

  public T setBootstrapOptions(Map<ChannelOption<Object>, Object> options) {
    this.options = options;
    return (T) this;
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

  public T setXioName(String XioName) {
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
}
