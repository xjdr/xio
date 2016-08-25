package com.xjeffrose.xio.storage;

import com.xjeffrose.xio.marshall.Marshallable;
import com.xjeffrose.xio.marshall.Unmarshaller;

public abstract class ReadProvider {

  private Unmarshaller unmarshaller;

  public ReadProvider(Unmarshaller unmarshaller) {
    this.unmarshaller = unmarshaller;
  }

  public abstract byte[] read(String key);

  public abstract boolean exists(String key);

  public boolean read(String key, Marshallable message) {
    if (exists(key)) {
      message.putBytes(unmarshaller, read(key));
      return true;
    }
    return false;
  }

}
