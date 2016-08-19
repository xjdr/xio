package com.xjeffrose.xio.storage;

import com.xjeffrose.xio.marshall.Marshallable;
import com.xjeffrose.xio.marshall.Unmarshaller;

public abstract class ReadProvider {

  private Unmarshaller unmarshaller;

  public ReadProvider(Unmarshaller unmarshaller) {
    this.unmarshaller = unmarshaller;
  }

  public abstract byte[] read(String keyName);

  public abstract boolean exists(String keyName);

  public boolean read(Marshallable message) {
    if (exists(message.keyName())) {
      message.putBytes(unmarshaller, read(message.keyName()));
      return true;
    }
    return false;
  }

}
