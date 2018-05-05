package com.xjeffrose.xio.config;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.annotations.VisibleForTesting;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConfigReloader<T> {

  private static class Meta<T> {
    final T value;
    final String path;
    final long lastModified;
    final byte[] digest;

    Meta(T value, String path, long lastModified, byte[] digest) {
      this.value = value;
      this.path = path;
      this.lastModified = lastModified;
      this.digest = digest;
    }
  }

  private final ScheduledExecutorService executor;
  private final Function<Config, T> factory;
  private BiConsumer<T, T> updater;
  private Meta<T> metadata;
  private Map<String, Long> watchFiles = new HashMap<String, Long>();

  public ConfigReloader(ScheduledExecutorService executor, Function<Config, T> factory) {
    this.executor = executor;
    this.factory = factory;
    this.updater = null;
  }

  private Reader buildDigestReader(File file, MessageDigest digest) throws FileNotFoundException {
    DigestInputStream digester = new DigestInputStream(new FileInputStream(file), digest);
    return new InputStreamReader(digester);
  }

  private Config parse(Reader reader) {
    Config config = ConfigFactory.parseReader(reader);
    return ConfigFactory.load(config);
  }

  private Meta<T> load(File file) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      T value = factory.apply(parse(buildDigestReader(file, digest)));
      return new Meta<T>(value, file.getAbsolutePath(), file.lastModified(), digest.digest());
    } catch (Exception e) {
      throw new IllegalStateException(
          "Couldn't load config from file '" + file.getAbsolutePath() + "'", e);
    }
  }

  private Meta<T> load(String file) {
    return load(new File(file));
  }

  private boolean performUpdate(Meta<T> oldValue, Meta<T> newValue) {
    return !MessageDigest.isEqual(oldValue.digest, newValue.digest);
  }

  private boolean haveWatchFilesChanged() {
    System.out.println("watching");
    Boolean filesHaveChanged = false;
    try {
      Set<String> keys = new HashSet<String>(watchFiles.keySet());
      for (String filePath : keys) {
        File path = new File(filePath);
        System.out.println("path = " + path.getAbsoluteFile());
        System.out.println("filePath = " + filePath);
        Scanner input = new Scanner(path);
        while (input.hasNextLine())
        {
          System.out.println(input.nextLine());
        }


        long previousModifiedDate = watchFiles.get(filePath);
        long currentModifiedDate = path.lastModified();
        if (currentModifiedDate > previousModifiedDate) {
          // update the watchFile
          filesHaveChanged = true;
          watchFiles.put(filePath, currentModifiedDate);
        }
      }
    }
    catch(Exception e) {
      log.error("Caught exception while checking for if watch files have changed", e);
    }
    return filesHaveChanged;
  }

  @VisibleForTesting
  void checkForUpdates() {
    try {
      // check to see if any of the specific watch files have changed
      if (haveWatchFilesChanged()) {
        File path = new File(metadata.path);
        // suck up the original file which includes all the sub files
        ConfigFactory.invalidateCaches();
        Meta<T> update = load(path);
        updater.accept(metadata.value, update.value);
        metadata = update;
      } else {
        log.debug("No update: None of the watch files have changed", metadata.path);
      }
    } catch (Exception e) {
      log.error("Caught exception while checking for updates", e);
    }
  }

  public T init(String file) {
    metadata = load(file);
    return metadata.value;
  }

  @VisibleForTesting
  void setUpdater(BiConsumer<T, T> updater) {
    this.updater = updater;
  }

  public void addWatchFile(String filePath) {
    if (!watchFiles.containsKey(filePath)) {
      File file = new File(filePath);
      watchFiles.put(filePath, file.lastModified());
    } else {
      log.error("Attempted to add duplicate watch file: " + filePath);
    }
  }

  public void start(BiConsumer<T, T> updater) {
    checkNotNull(updater, "updater cannot be null");
    checkNotNull(metadata, "init must be called before start");
    checkState(this.updater == null, "start cannot be called more than once");
    setUpdater(updater);
    executor.scheduleWithFixedDelay(this::checkForUpdates, 2000, 2000, TimeUnit.MILLISECONDS);
  }
}
