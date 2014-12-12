package com.xjeffrose.xio;

import java.nio.channels.*;
import java.util.concurrent.*;

class Terminator implements Callable<Boolean>, Connection.Delegate {

  private final Connection connection;
  private final CryptoEngine engine;

  Terminator(Connection connection) {
    this.connection = connection;
    connection.setDelegate(this);
    engine = new CryptoEngine();
  }

  /* i/o thread */
  @Override public void read(SocketChannel channel) {
    // push onto readQueue
  }

  /* i/o thread */
  @Override public void write(SocketChannel channel) {
    // pull from writeQueue
  }

  /* executor thread */
  @Override public Boolean call() throws Exception {
    /*
    while (good) {
      while ( readQueue.notEmpty() ) {
        engine.decrypt(readQueue);
      }
      while ( encryptionQueue.notEmpty() ) {
        writeQueue.push(engine.encrypt(encryptionQueue));
      }
    }
    */
    return true;
  }

}
