package com.xjeffrose.xio.core;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.util.HashedWheelTimer;
import java.io.Closeable;
import java.util.concurrent.TimeUnit;
import javax.annotation.PreDestroy;

public final class XioTimer extends HashedWheelTimer implements Closeable {
  public XioTimer(String prefix, long tickDuration, TimeUnit unit, int ticksPerWheel) {
    super(new ThreadFactoryBuilder().setNameFormat(prefix + "-timer-%s").setDaemon(true).build(),
        tickDuration,
        unit,
        ticksPerWheel);
  }

  public XioTimer(String prefix) {
    this(prefix, 100, TimeUnit.MILLISECONDS, 512);
  }

  @PreDestroy
  @Override
  public void close() {
    stop();
  }
}