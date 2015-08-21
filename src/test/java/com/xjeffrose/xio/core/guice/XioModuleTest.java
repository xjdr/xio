package com.xjeffrose.xio.core.guice;

import com.google.inject.Guice;
import com.google.inject.Stage;
import com.xjeffrose.xio.core.HttpServerDef;
import com.xjeffrose.xio.core.HttpServerDefBuilder;
import com.xjeffrose.xio.core.XioBootstrap;
import org.junit.Test;

import static org.junit.Assert.*;

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
            HttpServerDef serverDef = new HttpServerDefBuilder()
                .build();

            // Bind the definition
            bind().toInstance(serverDef);
          }
        }).getInstance(XioBootstrap.class);

    // Start the server
    bootstrap.start();

    // Arrange to stop the server at shutdown
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        bootstrap.stop();
      }
    });

  }
}