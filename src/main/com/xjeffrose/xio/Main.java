package com.xjeffrose.xio;

import java.io.*;
import java.util.*;

class Main {
  public static void main(String[] args) {
    System.out.println("Hello");
    try {
        Server s = new Server(8080);
    } catch (IOException e) {
        e.printStackTrace();
    }

  }

}
