package com.xjeffrose.xio.helpers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class EchoServer implements AutoCloseable {
  ServerSocket serverSocket = null;
  Socket peerSocket = null;
  PrintWriter out = null;
  BufferedReader in = null;

  public void bind(InetSocketAddress address) {
    try {
      serverSocket = new ServerSocket();
      serverSocket.bind(address);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public InetSocketAddress addressBound() {
    return (InetSocketAddress) serverSocket.getLocalSocketAddress();
  }

  public void accept() {
    try {
      peerSocket = serverSocket.accept();
      out = new PrintWriter(peerSocket.getOutputStream(),true);
      in = new BufferedReader(new InputStreamReader(peerSocket.getInputStream()));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public String process() {
    try {
      String inputLine = in.readLine();
      out.println(inputLine);
      return inputLine;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void close() {
    try {
      out.close();
      in.close();
      peerSocket.close();
      serverSocket.close();
    } catch(IOException e) {
      throw new RuntimeException(e);
    }
  }
}
