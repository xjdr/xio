package com.xjeffrose.xio.helpers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

public class EchoClient implements AutoCloseable {
  Socket echoSocket = null;
  PrintWriter out = null;
  BufferedReader in = null;

  public void connect(InetSocketAddress address) {
    try {
      echoSocket = new Socket();
      echoSocket.connect(address);
      out = new PrintWriter(echoSocket.getOutputStream(), true);
      in = new BufferedReader(new InputStreamReader(echoSocket.getInputStream()));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void send(String payload) {
    out.println(payload);
  }

  public String recv() {
    try {
      return in.readLine();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void close() {
    try {
      out.close();
      in.close();
      echoSocket.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
