package com.xjeffrose.xio;

import java.io.*;
/* import java.net.*; */
import java.nio.*;
import java.nio.channels.*;
/* import java.nio.charset.*; */
import java.util.logging.*;

import com.xjeffrose.log.*;

class ChannelContext {
  private static final Logger log = Log.getLogger(ChannelContext.class.getName());

  public final SocketChannel channel;
  public final ChannelBuffer cb = new ChannelBuffer();
  public HttpParser parser = new HttpParser();
  private boolean readyToWrite = false;
  private final String outputPayload = new String("HTTP/1.1 200 OK\r\n" +
                                        "Content-Length: 40\r\n" +
                                        "Content-Type: text/html\r\n" +
                                        "\r\n\r\n" +
                                        "<html><body>HELLO WORLD!</body></html>");

  ChannelContext(SocketChannel channel) {
    this.channel = channel;

    //log.info("ChannelContext Created");
  }

  public void read() {
    int nread = 1;
    while (nread > 0) {
      try {
        nread = channel.read(cb.bb);
        if (!parser.parse(cb.bb)) {
          throw new RuntimeException("Parser Failed to Parse");
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      if (nread > 1020) {
        cb.addStream();
        nread = 1;
      }
      if (nread == -1) {
        try {
          //log.info("Closing Channel " + channel);
          channel.close();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    }
    cb.addStream();

    /* super_naive_proxy(cb.toString()); */
    /* super_naive_output(); */
    readyToWrite = true; //Neet to make this Future<boolean>
  }

  public void write() {
    try {
      if(readyToWrite) { // Need to do the blocking Future<boolean>.get() here
        /* channel.write(cb.getStream()); // Is an EchoServer for Testing */
        channel.write(ByteBuffer.wrap(outputPayload.getBytes())); // COMMENT LINE OUT: This is Only for testing
        channel.close();
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void write(ByteBuffer bb) {
    try {
      if (readyToWrite) {
        channel.write(bb);
        channel.close();
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void write(ChannelBuffer cbOut) {
    try {
      if (readyToWrite) {
        channel.write(cbOut.getStream());
        channel.close();
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /* private void super_naive_proxy(String payload) { */
  /*   //log.info("HERE WE GO BRO: " + payload); */
  /*   byte[] pbites = new byte[1024];// = 1024; */
  /*   try{ */
  /*     InetAddress address = InetAddress.getByName("127.0.0.1"); */
  /*     Socket proxy = new Socket(address, 8000); */
  /*     proxy.getOutputStream().write(payload.getBytes("UTF-8")); */
  /*     boolean all_done = false; */
  /*     while (!all_done) { */
  /*       int bytes_read = proxy.getInputStream().read(pbites); */
  /*       return_payload.append((new String(pbites, Charset.forName("UTF-8"))).substring(0,bytes_read)); */
  /*       ///channel.write(ByteBuffer.wrap(pbites, 0, bytes_read)); */
  /*       //log.info("bytes_read: " + bytes_read); */
  /*       //log.info("payload length: " + return_payload.toString().length()); */
  /*       if (return_payload.indexOf("\r\n\r\n") != -1) { */
  /*         //log.info("separator at: " + return_payload.indexOf("\r\n\r\n")); */
  /*         String[] parts = return_payload.toString().split("\r\n\r\n"); */
  /*         String[] headers = parts[0].split("\r\n"); */
  /*         for (String header : headers) { */
  /*           //log.info("HEADER " + header); */
  /*           if (header.contains("Content-Length")) { */
  /*             int length = Integer.parseInt(header.split(": ")[1]); */
  /*             //log.info("length " + length + " parts " + parts[1].length()); */
  /*             if (parts[1].length() >= length) { */
  /*               all_done = true; */
  /*               readyToWrite = true; */
  /*               break; */
  /*             } */
  /*           } */
  /*         } */
  /*       } */
  /*     } */
  /*     proxy.close(); */
  /*   } catch (IOException e) { */
  /*     throw new RuntimeException(e); */
  /*   } */
  /* } */


}
