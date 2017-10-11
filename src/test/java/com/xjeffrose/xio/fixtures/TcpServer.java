package com.xjeffrose.xio.fixtures;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class TcpServer implements Runnable {

  private int port;

  public TcpServer(int port) {

    this.port = port;
  }

  @Override
  public void run() {
    try {
      ServerSocket socket = new ServerSocket(port);
      Socket connectionSocket = socket.accept();

      BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
      PrintWriter out = new PrintWriter(connectionSocket.getOutputStream(), true);
      String req = inFromClient.readLine();
      //For debug only
//      System.out.println("RECIEED REQ: " + req);
      out.println(req);

      connectionSocket.close();
      socket.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
