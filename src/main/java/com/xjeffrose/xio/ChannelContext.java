package com.xjeffrose.xio;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import java.util.logging.*;

import com.xjeffrose.log.*;

abstract class ChannelContext {
  private static final Logger log = Log.getLogger(ChannelContext.class.getName());

  public final SocketChannel channel;

  ChannelContext(SocketChannel channel) {
    this.channel = channel;
  }

  abstract public void read();

  abstract public void write();

}
