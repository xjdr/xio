package com.xjeffrose.util;

import java.util.concurrent.*;

public interface Awaitable extends Callable {

  @Override public Object call() throws Exception;

  public void ready();

  public void result();

  public void isReady();

}
