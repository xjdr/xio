package com.xjeffrose.xio.client.retry;

import java.util.concurrent.TimeUnit;

/**
 * Mechanism for timing methods and recording counters
 */
public interface TracerDriver {
  /**
   * Record the given trace event
   *
   * @param name of the event
   * @param time time event took
   * @param unit time unit
   */
  public void addTrace(String name, long time, TimeUnit unit);

  /**
   * Add to a named counter
   *
   * @param name name of the counter
   * @param increment amount to increment
   */
  public void addCount(String name, int increment);
}