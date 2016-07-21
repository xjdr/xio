package com.xjeffrose.xio.storage;

import com.xjeffrose.xio.marshall.Marshallable;
import com.xjeffrose.xio.marshall.Marshaller;

public abstract class WriteProvider {

  private Marshaller marshaller;

  public WriteProvider(Marshaller marshaller) {
    this.marshaller = marshaller;
  }

  public abstract void write(String className, byte[] bytes);

  public void write(Marshallable message) {
    write(message.keyName(), message.getBytes(marshaller));
  }

}
