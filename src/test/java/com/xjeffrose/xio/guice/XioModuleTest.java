package com.xjeffrose.xio.guice;

import com.google.inject.Guice;
import com.google.inject.Stage;
import com.xjeffrose.xio.server.XioServerDef;
import com.xjeffrose.xio.server.XioServerDefBuilder;
import com.xjeffrose.xio.server.XioBootstrap;
import org.junit.Test;

public class XioModuleTest {

  @Test
  public void testBind() throws Exception {

    final XioBootstrap bootstrap = Guice.createInjector(
        Stage.PRODUCTION,
        new XioModule() {
          @Override
          protected void configureXio() {
//              // Create the handler
//              MyService.Iface serviceInterface = new MyServiceHandler();
//
//              // Create the processor
//              TProcessor processor = new MyService.Processor<>(serviceInterface);
//
//              // Build the server definition
            XioServerDef serverDef = new XioServerDefBuilder()
                .build();

            // Bind the definition
            bind().toInstance(serverDef);
          }
        }).getInstance(XioBootstrap.class);

    // Start the server
    bootstrap.start();

    //For testing only (LEAVE OUT)
//    Thread.sleep(200000000);

    // Arrange to stop the server at shutdown
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        bootstrap.stop();
      }
    });

  }
}