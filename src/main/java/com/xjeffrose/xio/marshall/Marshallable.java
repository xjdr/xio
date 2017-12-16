package com.xjeffrose.xio.marshall;

public interface Marshallable {

  public String keyName();

  public byte[] getBytes(Marshaller marshaller);

  public void putBytes(Unmarshaller unmarshaller, byte[] data);
}
