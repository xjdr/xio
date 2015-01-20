package com.xjeffrose.xio;

import java.io.*;
import java.nio.*;
import java.nio.charset.*;
import java.util.concurrent.*;
import java.util.logging.*;

/* import sun.nio.ch.*; */

import com.xjeffrose.log.*;

class ChannelBuffer {
  private static final Logger log = Log.getLogger(ChannelBuffer.class.getName());

  public final ByteBuffer bb = ByteBuffer.allocateDirect(1024);
  private final ByteBuffer[] stream = new ByteBuffer[256];
  private int streamIndex = 0;

  public int position() {
    return bb.position();
  }

  public int limit() {
    return bb.limit();
  }

  private ByteBuffer slice(final int position, final int limit) {
    final ByteBuffer temp = bb.duplicate();
    temp.position(position);
    temp.limit(limit);
    final ByteBuffer slice = temp.slice();
    return slice;
  }

  public String toString() {
     final ByteBuffer temp = bb.duplicate();
     temp.flip();
     return new String(temp.asCharBuffer().toString().getBytes(), Charset.forName("UTF-8"));
  }

  public static String toString(ByteBuffer tempbb) {
     final ByteBuffer temp = tempbb.duplicate();
     temp.flip();
     return new String(temp.asCharBuffer().toString().getBytes(), Charset.forName("UTF-8"));
  }

  public String toString(Charset charset, int position, int limit) {
     final ByteBuffer temp = bb.duplicate();
     temp.position(position);
     temp.limit(limit);
    try {
      return new String(temp.asCharBuffer().toString().getBytes(), charset.name());
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException(e);
    }
  }

  public ByteBuffer copy() {
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

  public void addStream(){
    if (streamIndex > 254) {
      throw new RuntimeException("Exceeded ChannelBuffer Stream Capacity");
    }
    final ByteBuffer temp = copy();
    stream[streamIndex] = temp;
    streamIndex++;
    bb.flip();
    bb.clear();
  }

  public ByteBuffer get() {
    ByteBuffer temp = bb.duplicate();
    temp.flip();
    return temp;
  }

  public ByteBuffer[] getStream() {
    return stream;
  }

  public byte[] get(final int position, final int limit) {
    final byte[] temp = new byte[256];
    bb.get(temp, position, limit + 1);
    return temp;
  }

  /* private void put(byte[] bite) { */
  /*   bb.put(bite); */
  /* } */
  /*  */
  /* private void put(byte[] bite, final int position, final int offset) { */
  /* } */

  public void put(ByteBuffer buf) {
    bb.put(buf);
  }

  /* private void put(ByteBuffer buf, final int position, final int offset) { */
  /* } */

}
