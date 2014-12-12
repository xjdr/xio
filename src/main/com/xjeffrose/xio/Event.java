package com.xjeffrose.xio;

import java.io.*;

interface Event {

  void registerWithEventLoop(EventLoop loop) throws IOException;

}
