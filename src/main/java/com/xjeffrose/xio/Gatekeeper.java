package com.xjeffrose.xio;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.logging.*;

import com.xjeffrose.log.*;

class Gatekeeper implements Runnable {
  private static final Logger log = Log.getLogger(Gatekeeper.class.getName());

  private final AtomicBoolean isRunning = new AtomicBoolean();
  private SelectionKey key;
  private SocketChannel channel;

  private final boolean ssl = false; //for debug will remove

  Gatekeeper() {
    isRunning.set(true);
  }

  public void close() {
    isRunning.set(false);
  }

  public void accept(SelectionKey key) {
    this.key = key;

    try {
      ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
      channel = ssc.accept();
      SocketAddress remoteAddress = channel.getRemoteAddress();
      log.info("remote address: " + remoteAddress.toString());

      if (channel == null) {
        log.info("Dropping null channel " + channel + " " + key.isAcceptable());
        killClient();
      }

      else if (IpFilter.filter(remoteAddress)) {
        log.info("Dropping connection" + channel + " Based on IpFilter rule ");
        killClient();
      }

      else if (RateLimit.limit()) {
        log.info("Dropping " + channel + " Due to RateLiming");
        killClient();
      }

      else log.info("Accepted Connection from " + channel);


    } catch (IOException e) {
      log.info("There was an error here with " + key.channel());
      key.cancel();
    }

  }

  public void acceptor(SelectionKey key) {
    try {
      ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
      channel = ssc.accept();
      SocketAddress remoteAddress = channel.getRemoteAddress();
      if(channel == null) {
        log.info("Dropping null channel " + channel + " " + key.isAcceptable());
        killClient();
      } 
      else log.info("Accepted Connection from " + channel);
    } catch (IOException e) {
    }
  }

  public void ipFilter(SelectionKey key) {
    if(false) {
      killClient();
    }
  }

  public void rateLimit(SelectionKey key) {
    if(false) {
      killClient();
    }
  }

 private void killClient() {
    try {
      channel.close();
      key.cancel();
    } catch (IOException e) {
    }
  }

  @Override public void run() {
    if (ssl) {
      Terminator terminator = new Terminator(channel);
    }
    Session session = new Session(channel);
  }

}
