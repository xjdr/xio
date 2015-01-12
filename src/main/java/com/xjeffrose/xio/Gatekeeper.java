package com.xjeffrose.xio;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.concurrent.*;
import java.util.logging.*;

import com.xjeffrose.log.*;

class Gatekeeper {
  private static final Logger log = Log.getLogger(Gatekeeper.class.getName());

  private SelectionKey key;
  private SocketChannel channel;
  private SocketAddress remoteAddress;
  private IOService[] ioPool;

  private final boolean ssl = false; //for debug will remove

  Gatekeeper() {
  }

  public void accept(SelectionKey key) {
    try {
      ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
      channel = ssc.accept();
      remoteAddress = channel.getRemoteAddress();
      if(channel == null) {
        log.info("Dropping null channel " + channel + " " + key.isAcceptable());
        killClient();
      }
      log.info("Accepted Connection from " + channel);
      run();
    } catch (IOException e) {
    }
  }

  public void ipFilter() {
    IpFilter ipf = new IpFilter();
    if(ipf.filter(remoteAddress)) {
      killClient();
    }
  }

  public void rateLimit() {
    RateLimit rl = new RateLimit();
    if(rl.limit()) {
      killClient();
    }
  }

  public void ioPool(IOService[] ioPool) {
    this.ioPool = ioPool;
  }

  private void scheduleIO(SocketChannel channel) {
    ioPool[0].register(channel);
  }

  private void killClient() {
    try {
      channel.close();
      key.cancel();
    } catch (IOException e) {
    }
  }

  public void run() {
    scheduleIO(channel);
  }

}
