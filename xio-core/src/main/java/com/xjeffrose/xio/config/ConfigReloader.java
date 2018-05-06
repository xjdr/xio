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
import java.security.NoSuchAlgorithmException;
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

  private static class ConfigFileMetadata {
    final String path;
    final long lastModified;
    final byte[] digest;

    ConfigFileMetadata(String path, long lastModified, byte[] digest) {
      this.path = path;
      this.lastModified = lastModified;
      this.digest = digest;
    }
  }

  private final ScheduledExecutorService executor;
  private final Function<Config, T> factory;
  private BiConsumer<T, T> updater;
  private Meta<T> metadata;
  private Map<String, ConfigFileMetadata> watchFiles = new HashMap<String, ConfigFileMetadata>();

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

  private boolean performUpdate(ConfigFileMetadata current, ConfigFileMetadata previous) {
    return (current.lastModified > previous.lastModified &&
            !MessageDigest.isEqual(current.digest, previous.digest));
  }

  private ConfigFileMetadata loadMetaData(File configFile) throws IllegalStateException {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      DigestInputStream digester = new DigestInputStream(new FileInputStream(configFile), digest);
      ConfigFactory.parseReader(new InputStreamReader(digester));
      return new ConfigFileMetadata(configFile.getAbsolutePath(), configFile.lastModified(), digest.digest());
    } catch (FileNotFoundException e) {
      throw new IllegalStateException(
        "Couldn't load config from file '" + configFile.getAbsolutePath() + "'", e);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(
        "Illegal digest algorithm used on file '" + configFile.getAbsolutePath() + "'", e);
    }
  }

  private boolean haveWatchFilesChanged() {
    Boolean filesHaveChanged = false;
    try {
      Set<String> keys = new HashSet<String>(watchFiles.keySet());
      for (String filePath : keys) {
        File file = new File(filePath);
        ConfigFileMetadata currentFileMetadata = loadMetaData(file);
        ConfigFileMetadata previousConfigMetadata = watchFiles.get(filePath);
        if (performUpdate(currentFileMetadata, previousConfigMetadata)) {
          // update the watchFile
          filesHaveChanged = true;
          watchFiles.put(filePath, currentFileMetadata);
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

  public void addWatchFile(File file) throws IllegalStateException {
    if (!watchFiles.containsKey(file.getAbsolutePath())) {
      ConfigFileMetadata configMetaData = loadMetaData(file);
      watchFiles.put(file.getAbsolutePath(), configMetaData);
    } else {
      log.error("Attempted to add duplicate watch file: " + file.getAbsolutePath());
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
