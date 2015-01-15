package com.xjeffrose.xio;

import java.io.*;
import java.nio.*;
import java.nio.charset.*;
import java.util.concurrent.*;
import java.util.logging.*;

import com.xjeffrose.log.*;

class ChannelBuffer extends ByteBuffer {
  private static final Logger log = Log.getLogger(ChannelBuffer.class.getName());

  /* public final ByteBuffer bb = ByteBuffer.allocateDirect(1024); */
  private final ByteBuffer[] stream = new ByteBuffer[256];
  private int streamIndex = 0;

  ChannelBuffer() {
    this.allocateDirect(1024);
  }

  private ByteBuffer slice(final int position, final int limit) {
    final ByteBuffer temp = this.duplicate();
    temp.position(position);
    temp.limit(limit);
    final ByteBuffer slice = temp.slice();
    return slice;
  }

  /* public static ByteBuffer allocateDirect(int capacity) { */
  /*   return new DirectByteBuffer(capacity); */
  /* } */

  @Override
   public String toString() {
     final ByteBuffer temp = this.duplicate();
     temp.flip();
     return new String(temp.asCharBuffer().toString().getBytes(), Charset.forName("UTF-8"));
  }

  private String toString(Charset charset, int position, int limit) {
     final ByteBuffer temp = this.duplicate();
     temp.position(position);
     temp.limit(limit);
    try {
      return new String(temp.asCharBuffer().toString().getBytes(), charset.name());
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException(e);
    }
  }

  private ByteBuffer copy() {
    final ByteBuffer copy = ByteBuffer.allocateDirect(this.capacity());
    final ByteBuffer temp = this.duplicate();
    final int pos = this.position();
    final int lim = this.limit();
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
    this.flip();
    this.clear();
  }

  /* @Override */
  /* public byte[] get() { */
  /*   return new byte[1]; */
  /* } */
  /*  */

  private byte[] get(final int position, final int offset) {
    return new byte[1];
  }

  /* private void put(byte[] bite) { */
  /* } */
  /*  */
  /* private void put(byte[] bite, final int position, final int offset) { */
  /* } */

  @Override
  public ByteBuffer put(ByteBuffer buf) {
  }

  private void put(ByteBuffer buf, final int position, final int offset) {
  }


}
