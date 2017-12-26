package com.xjeffrose.xio.fixtures;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class TcpClient {

  public static String sendReq(String host, int port, String req) {

    try {
      Socket clientSocket = new Socket(host, port);
      DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
      outToServer.writeBytes(req + '\n');

      BufferedReader inFromServer =
          new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

      String response = inFromServer.readLine();
      clientSocket.close();

      return response;

    } catch (IOException e) {
      e.printStackTrace();
    }

    return null;
  }
}
