package com.xjeffrose.xio.storage;

import com.xjeffrose.xio.marshall.Marshallable;
import com.xjeffrose.xio.marshall.Marshaller;

public abstract class WriteProvider {

  private Marshaller marshaller;

  public WriteProvider(Marshaller marshaller) {
    this.marshaller = marshaller;
  }

  public abstract void write(String key, byte[] bytes);

  public void write(String key, Marshallable message) {
    write(key, message.getBytes(marshaller));
  }
}
