package com.xjeffrose.xio.config;

import static io.netty.handler.codec.http.HttpMethod.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.*;

import com.typesafe.config.Config;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.function.BiConsumer;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ConfigReloaderUnitTest extends Assert {

  @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();

  String createConfig(String value) throws IOException, FileNotFoundException {
    File output = new File(temporaryFolder.getRoot(), "application.conf");

    PrintStream out = new PrintStream(output);
    out.append("limit=").append(value).println();
    out.flush();
    out.close();
    return output.getAbsolutePath();
  }

  public static class TrivialConfig {
    final int limit;

    TrivialConfig(Config config) {
      limit = config.getInt("limit");
    }
  }

  public static class TrivialState {
    TrivialConfig config;

    TrivialState(TrivialConfig config) {
      this.config = config;
    }

    public void update(TrivialConfig oldValue, TrivialConfig newValue) {
      this.config = newValue;
    }

    public void fireUpdated() {}
  }

  @Test
  public void testWiring() throws Exception {
    ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(1);
    ConfigReloader<TrivialConfig> reloader = new ConfigReloader<>(executor, TrivialConfig::new);
    TrivialConfig config = reloader.init(createConfig("10"));
    TrivialState state =
        new TrivialState(config) {
          @Override
          public void fireUpdated() {
            assertEquals(20, this.config.limit);
            executor.shutdown();
          }
        };
    assertEquals(10, config.limit);
    reloader.start(state::update);

    Thread.sleep(2000);

    createConfig("20");
  }

  @Test
  public void testInitHappyPath() throws Exception {
    ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(1);
    ConfigReloader<TrivialConfig> reloader = new ConfigReloader<>(executor, TrivialConfig::new);
    TrivialConfig config = reloader.init(createConfig("10"));
    assertEquals(10, config.limit);
  }

  @Test(expected = IllegalStateException.class)
  public void testInitBadValue() throws Exception {
    ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(1);
    ConfigReloader<TrivialConfig> reloader = new ConfigReloader<>(executor, TrivialConfig::new);
    TrivialConfig config = reloader.init(createConfig("str"));
  }

  @Test
  public void testReloadHappyPath() throws Exception {
    ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(1);
    BiConsumer<TrivialConfig, TrivialConfig> updater =
        (oldValue, newValue) -> {
          assertEquals(10, oldValue.limit);
          assertEquals(20, newValue.limit);
        };
    ConfigReloader<TrivialConfig> reloader = new ConfigReloader<>(executor, TrivialConfig::new);
    reloader.setUpdater(updater);
    TrivialConfig config = reloader.init(createConfig("10"));
    assertEquals(10, config.limit);

    Thread.sleep(2000);

    createConfig("20");

    reloader.checkForUpdates();
  }

  @Test
  public void testReloadHappyPathStaleTimestamp() throws Exception {
    ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(1);
    BiConsumer<TrivialConfig, TrivialConfig> updater =
        (oldValue, newValue) -> {
          assertEquals(10, oldValue.limit);
          assertEquals(20, newValue.limit);
        };
    ConfigReloader<TrivialConfig> reloader = new ConfigReloader<>(executor, TrivialConfig::new);
    reloader.setUpdater(updater);
    TrivialConfig config = reloader.init(createConfig("10"));
    assertEquals(10, config.limit);

    Thread.sleep(2000);

    reloader.checkForUpdates();
  }

  @Test
  public void testReloadHappyPathStaleContent() throws Exception {
    ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(1);
    BiConsumer<TrivialConfig, TrivialConfig> updater =
        (oldValue, newValue) -> {
          assertEquals(10, oldValue.limit);
          assertEquals(20, newValue.limit);
        };
    ConfigReloader<TrivialConfig> reloader = new ConfigReloader<>(executor, TrivialConfig::new);
    reloader.setUpdater(updater);
    TrivialConfig config = reloader.init(createConfig("10"));
    assertEquals(10, config.limit);

    Thread.sleep(2000);

    createConfig("10");

    reloader.checkForUpdates();
  }

  @Test
  public void testReloadBadValue() throws Exception {
    ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(1);
    BiConsumer<TrivialConfig, TrivialConfig> updater =
        (oldValue, newValue) -> {
          assertTrue(false);
        };
    ConfigReloader<TrivialConfig> reloader = new ConfigReloader<>(executor, TrivialConfig::new);
    reloader.setUpdater(updater);
    TrivialConfig config = reloader.init(createConfig("10"));
    assertEquals(10, config.limit);

    Thread.sleep(2000);

    createConfig("str");

    reloader.checkForUpdates();
  }

  @Test(expected = NullPointerException.class)
  public void testStartWithoutInit() throws Exception {
    ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(1);
    BiConsumer<TrivialConfig, TrivialConfig> updater =
        (oldValue, newValue) -> {
          assertTrue(true);
        };
    ConfigReloader<TrivialConfig> reloader = new ConfigReloader<>(executor, TrivialConfig::new);
    reloader.start(updater);
  }

  @Test
  public void testStartHappyPath() throws Exception {
    final ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(1);
    BiConsumer<TrivialConfig, TrivialConfig> updater =
        (oldValue, newValue) -> {
          assertTrue(true);
        };
    ConfigReloader<TrivialConfig> reloader =
        new ConfigReloader<TrivialConfig>(executor, TrivialConfig::new) {
          @Override
          public void checkForUpdates() {
            executor.shutdown();
          }
        };
    TrivialConfig config = reloader.init(createConfig("10"));
    reloader.start(updater);
  }
}
