package com.xjeffrose.xio.storage;

import com.xjeffrose.xio.marshall.Marshallable;
import com.xjeffrose.xio.marshall.Unmarshaller;

public abstract class ReadProvider {

  private Unmarshaller unmarshaller;

  public ReadProvider(Unmarshaller unmarshaller) {
    this.unmarshaller = unmarshaller;
  }

  public abstract byte[] read(String keyName);

  public void read(Marshallable message) {
    message.putBytes(unmarshaller, read(message.keyName()));
  }

}
