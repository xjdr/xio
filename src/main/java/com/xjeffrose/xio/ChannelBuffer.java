package com.xjeffrose.xio;

import java.io.*;
import java.nio.*;
import java.nio.charset.*;
import java.util.concurrent.*;
import java.util.logging.*;

import com.xjeffrose.log.*;

class ChannelBuffer {
  private static final Logger log = Log.getLogger(ChannelBuffer.class.getName());

  public final ByteBuffer bb = ByteBuffer.allocateDirect(1024);
  private final ByteBuffer[] stream = new ByteBuffer[256];
  private int streamIndex = 0;

  private ByteBuffer slice(final int position, final int limit) {
    final ByteBuffer temp = bb.duplicate();
    temp.position(position);
    temp.limit(limit);
    final ByteBuffer slice = temp.slice();
    return slice;
  }

  @Override public String toString() {
     final ByteBuffer temp = bb.duplicate();
     temp.flip();
     return new String(temp.asCharBuffer().toString().getBytes(), Charset.forName("UTF-8"));
  }

  private String toString(Charset charset, int position, int limit) {
     final ByteBuffer temp = bb.duplicate();
     temp.position(position);
     temp.limit(limit);
    try {
      return new String(temp.asCharBuffer().toString().getBytes(), charset.name());
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException(e);
    }
  }

  private ByteBuffer copy() {
    final ByteBuffer copy = ByteBuffer.allocateDirect(bb.capacity());
    final ByteBuffer temp = bb.duplicate();
    final int pos = bb.position();
    final int lim = bb.limit();
    temp.flip();
    copy.put(temp);
    copy.position(pos);
    copy.limit(lim);
    return copy;
  }

  public ByteBuffer[] getStream() {
    return stream;
  }

  public void addStream(){
    if (streamIndex > 254) {
      throw new RuntimeException("Exceeded ChannelBuffer Stream Capacity");
    }
    final ByteBuffer temp = copy();
    stream[streamIndex] = temp;
    streamIndex++;
    bb.clear();
  }

  private byte[] get() {
    return new byte[1];
  }

  private byte[] get(final int position, final int offset) {
    return new byte[1];
  }

  private void put(byte[] bite) {
  }

  private void put(byte[] bite, final int position, final int offset) {
  }

  private void put(ByteBuffer buf) {
  }

  private void put(ByteBuffer buf, final int position, final int offset) {
  }


}
