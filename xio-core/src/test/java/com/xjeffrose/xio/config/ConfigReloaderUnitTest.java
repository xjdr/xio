package com.xjeffrose.xio.config;

import com.typesafe.config.Config;
import java.io.*;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

public class ConfigReloaderUnitTest extends Assert {

  @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private static final String applicationConf = "testApplication.conf";
  private static final String includeConf = "includeFile.conf";

  private String createApplicationConf() throws FileNotFoundException {
    String value = "trivial { include \"" + includeConf + "\" }";
    return rawCreateConf(value, applicationConf);
  }

  private String modifyIncludeConf(String param) throws FileNotFoundException {
    String value = "{ limit = " + param + " }";
    return rawCreateConf(value, includeConf);
  }

  private String rawCreateConf(String value, String filename) throws FileNotFoundException {
    File output = new File(temporaryFolder.getRoot(), filename);
    PrintStream out = new PrintStream(output);
    out.append(value).println();
    out.flush();
    out.close();
    return output.getAbsolutePath();
  }

  private String createConfig(String value) throws FileNotFoundException {
    File output = new File(temporaryFolder.getRoot(), applicationConf);
    PrintStream out = new PrintStream(output);
    out.append("limit=").append(value).println();
    out.flush();
    out.close();
    return output.getAbsolutePath();
  }

  public static class TrivialConfig {
    final int limit;

    TrivialConfig(Config config) {
      limit = config.getInt("trivial.limit");
    }
  }

  public static class TrivialState {
    TrivialConfig config;

    TrivialState(TrivialConfig config) {
      this.config = config;
    }

    public void update(TrivialConfig oldValue, TrivialConfig newValue) {
      this.config = newValue;
      fireUpdated();
    }

    public void fireUpdated() {}
  }

  private static void addPath(String s) throws Exception {
    File f = new File(s);
    URI u = f.toURI();
    URLClassLoader urlClassLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
    Class<URLClassLoader> urlClass = URLClassLoader.class;
    Method method = urlClass.getDeclaredMethod("addURL", URL.class);
    method.setAccessible(true);
    method.invoke(urlClassLoader, u.toURL());
  }

  @Before
  public void before() throws Exception {
    addPath(temporaryFolder.getRoot().toString());
  }

  @Test
  public void testInitHappyPath() throws Exception {
    String initialLimit = "9000";
    String applicationConf = createApplicationConf();
    modifyIncludeConf(initialLimit);
    ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(1);
    ConfigReloader<TrivialConfig> reloader = new ConfigReloader<>(executor, TrivialConfig::new);
    TrivialConfig config = reloader.init(applicationConf);
    assertEquals(Integer.parseInt(initialLimit), config.limit);
  }

  @Test(expected = IllegalStateException.class)
  public void testInitBadValue() throws Exception {
    String initialLimit = "badvalue";
    String applicationConf = createApplicationConf();
    modifyIncludeConf(initialLimit);
    ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(1);
    ConfigReloader<TrivialConfig> reloader = new ConfigReloader<>(executor, TrivialConfig::new);
    TrivialConfig config = reloader.init(applicationConf);
  }

  @Test
  public void testReload_WhenWatchedFilesDoNotChange() throws Exception {
    AtomicBoolean fireUpdatedCalled = new AtomicBoolean(false);
    // set initial conditions for applicationConf and includeFileConf
    String initialLimit = "9000";
    String applicationConf = createApplicationConf();
    String includeFilePath = modifyIncludeConf(initialLimit);
    File includedFile = new File(includeFilePath);

    ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(1);

    ConfigReloader<TrivialConfig> reloader = new ConfigReloader<>(executor, TrivialConfig::new);

    TrivialConfig config = reloader.init(applicationConf);
    TrivialState state =
        new TrivialState(config) {
          @Override
          public void fireUpdated() {
            fireUpdatedCalled.set(true);
          }
        };

    // check that we successfully read the init'd subject
    assertEquals(Integer.parseInt(initialLimit), config.limit);
    // start watching the include file
    reloader.addWatchFile(includedFile);
    // kick off the subject
    reloader.start(state::update);
    Thread.sleep(5000);
    // check that we did not change the contents of the state since we didn't change the file
    assertEquals(Integer.parseInt(initialLimit), state.config.limit);
    // check to see that we did not call fireUpdated
    assertFalse(fireUpdatedCalled.get());
    executor.shutdown();
  }

  @Test
  public void testReload_WhenWatchedFilesChange_Date_Was_Modified_and_Digest_Was_NOT_Changed()
      throws Exception {
    AtomicBoolean fireUpdatedCalled = new AtomicBoolean(false);
    // set initial conditions for applicationConf and includeFileConf
    String initialLimit = "9000";
    String updatedLimit = "9000";
    String applicationConf = createApplicationConf();
    String includeFilePath = modifyIncludeConf(initialLimit);
    File includedFile = new File(includeFilePath);

    ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(1);

    ConfigReloader<TrivialConfig> reloader = new ConfigReloader<>(executor, TrivialConfig::new);

    TrivialConfig config = reloader.init(applicationConf);
    TrivialState state =
        new TrivialState(config) {
          @Override
          public void fireUpdated() {
            fireUpdatedCalled.set(true);
          }
        };

    // check that we successfully read the init'd subject
    assertEquals(Integer.parseInt(initialLimit), config.limit);
    // start watching the include file
    reloader.addWatchFile(includedFile);
    // kick off the subject
    reloader.start(state::update);
    Thread.sleep(5000);
    modifyIncludeConf(updatedLimit);
    Thread.sleep(5000);
    // check that we did not change the contents of the state since we didn't change the file contents
    assertEquals(Integer.parseInt(initialLimit), state.config.limit);
    // check to see that we did not call fireUpdated
    assertFalse(fireUpdatedCalled.get());
    executor.shutdown();
  }

  @Test
  public void testReload_WhenWatchedFilesChange_Date_Was_Modified_and_Digest_Was_Changed()
      throws Exception {
    // set initial conditions for applicationConf and includeFileConf
    String initialLimit = "9000";
    String updatedLimit = "9001";
    String applicationConf = createApplicationConf();
    String includeFilePath = modifyIncludeConf(initialLimit);
    File includedFile = new File(includeFilePath);

    ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(1);

    ConfigReloader<TrivialConfig> reloader = new ConfigReloader<>(executor, TrivialConfig::new);

    TrivialConfig config = reloader.init(applicationConf);
    TrivialState state =
        new TrivialState(config) {
          @Override
          public void fireUpdated() {
            executor.shutdown();
          }
        };

    // check that we successfully read the init'd subject
    assertEquals(Integer.parseInt(initialLimit), config.limit);
    // start watching the include file
    reloader.addWatchFile(includedFile);
    // kick off the subject
    reloader.start(state::update);
    // modify the watched file

    Thread.sleep(5000);
    modifyIncludeConf(updatedLimit);

    Thread.sleep(5000);
    // check to see that we successfully updated to the latest content of the include file
    assertEquals(Integer.parseInt(updatedLimit), state.config.limit);
  }

  @Test
  public void testReload_WhenWatchedFilesChange_BadUpdate() throws Exception {
    // set initial conditions for applicationConf and includeFileConf
    String initialLimit = "9000";
    String updatedLimit = "badvalue";
    String applicationConf = createApplicationConf();
    String includedFilePath = modifyIncludeConf(initialLimit);
    File includedFile = new File(includedFilePath);

    ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(1);

    ConfigReloader<TrivialConfig> reloader = new ConfigReloader<>(executor, TrivialConfig::new);

    TrivialConfig config = reloader.init(applicationConf);
    TrivialState state =
        new TrivialState(config) {
          @Override
          public void fireUpdated() {
            executor.shutdown();
          }
        };

    // check that we successfully read the init'd subject
    assertEquals(Integer.parseInt(initialLimit), config.limit);
    // start watching the include file
    reloader.addWatchFile(includedFile);
    // kick off the subject
    reloader.start(state::update);
    Thread.sleep(5000);
    // modify the watched file
    modifyIncludeConf(updatedLimit);
    Thread.sleep(5000);
    // if something goes wrong with the update we should not change the config (up for debate, the illegal
    // state exceptions are thrown in a different thread, do we want to just trap if we get a invalid config?
    assertEquals(Integer.parseInt(initialLimit), config.limit);
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
    BiConsumer<TrivialConfig, TrivialConfig> updater = (oldValue, newValue) -> {};
    ConfigReloader<TrivialConfig> reloader =
        new ConfigReloader<TrivialConfig>(executor, TrivialConfig::new) {
          @Override
          public void checkForUpdates() {
            executor.shutdown();
          }
        };
    String applicationConf = createApplicationConf();
    modifyIncludeConf("10");
    reloader.init(applicationConf);
    reloader.start(updater);
    Thread.sleep(5000);
    executor.shutdown();
  }
}
