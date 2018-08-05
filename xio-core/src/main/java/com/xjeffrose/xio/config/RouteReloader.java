package com.xjeffrose.xio.config;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.annotations.VisibleForTesting;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RouteReloader<T> {

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
  private final Function<String, T> factory;
  private BiConsumer<T, T> updater;
  private com.xjeffrose.xio.config.RouteReloader.Meta<T> metadata;
  private Map<String, com.xjeffrose.xio.config.RouteReloader.ConfigFileMetadata> watchFiles =
      new HashMap<String, com.xjeffrose.xio.config.RouteReloader.ConfigFileMetadata>();

  public static void main(String args[]) throws Exception {
    ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(1);
    RouteReloader x =
        new RouteReloader<ArrayList<DynamicRouteConfig>>(
            executor, DynamicRouteConfigsFactory::build);
    DynamicRouteConfigsFactory val = (DynamicRouteConfigsFactory) x.init(args[0]);
    x.start(
        (oldVal, newVal) -> {
          System.out.println(newVal);
        });
    Thread.sleep(100000);
  }

  public RouteReloader(ScheduledExecutorService executor, Function<String, T> factory) {
    this.executor = executor;
    this.factory = factory;
    this.updater = null;
  }

  public T init(String file) {
    metadata = load(file);
    addWatchFile(new File(file));
    return metadata.value;
  }

  private com.xjeffrose.xio.config.RouteReloader.Meta<T> load(String filePath) {
    final MessageDigest digest;
    try {
      digest = MessageDigest.getInstance("SHA-256");
    } catch (Exception e) {
      throw new IllegalStateException("Couldn't load config from file '" + filePath + "'", e);
    }
    try {
      File file = new File(filePath);
      byte[] encoded = Files.readAllBytes(Paths.get(filePath));
      String fileContents = new String(encoded, Charset.defaultCharset());
      T value = factory.apply(fileContents);
      digest.update(Files.readAllBytes(Paths.get(filePath)));
      return new com.xjeffrose.xio.config.RouteReloader.Meta<>(
          value, file.getAbsolutePath(), file.lastModified(), digest.digest());
    } catch (Exception e) {
      throw new IllegalStateException("Couldn't load config from file '" + filePath + "'", e);
    }
  }

  private boolean shouldPerformUpdate(
      com.xjeffrose.xio.config.RouteReloader.ConfigFileMetadata current,
      com.xjeffrose.xio.config.RouteReloader.ConfigFileMetadata previous) {
    return (current.lastModified > previous.lastModified
        && !MessageDigest.isEqual(current.digest, previous.digest));
  }

  private com.xjeffrose.xio.config.RouteReloader.ConfigFileMetadata loadMetaData(File configFile)
      throws IllegalStateException {
    final MessageDigest digest;
    try {
      digest = MessageDigest.getInstance("SHA-256");
    } catch (Exception e) {
      throw new IllegalStateException(
          "Couldn't load config from file '" + configFile.getAbsolutePath() + "'", e);
    }

    try {
      digest.update(Files.readAllBytes(Paths.get(configFile.getAbsolutePath())));
      return new com.xjeffrose.xio.config.RouteReloader.ConfigFileMetadata(
          configFile.getAbsolutePath(), configFile.lastModified(), digest.digest());
    } catch (Exception e) {
      throw new IllegalStateException(
          "Couldn't load config from file '" + configFile.getAbsolutePath() + "'", e);
    }
  }

  private boolean haveWatchFilesChanged() {
    try {
      Set<String> keys = new HashSet<String>(watchFiles.keySet());
      List<com.xjeffrose.xio.config.RouteReloader.ConfigFileMetadata> toUpdateList =
          keys.stream()
              .map(filePath -> loadMetaData(new File(filePath)))
              .filter(
                  currentMetaData ->
                      shouldPerformUpdate(currentMetaData, watchFiles.get(currentMetaData.path)))
              .collect(Collectors.toList());

      toUpdateList.forEach(update -> watchFiles.put(update.path, update));
      return !toUpdateList.isEmpty();
    } catch (Exception e) {
      log.error("Caught exception while checking for if watch files have changed", e);
    }
    return false;
  }

  @VisibleForTesting
  void checkForUpdates() {
    try {
      // check to see if any of the specific watch files have changed
      if (haveWatchFilesChanged()) {
        // suck up the original file which includes all the sub files
        com.xjeffrose.xio.config.RouteReloader.Meta<T> update = load(metadata.path);
        updater.accept(metadata.value, update.value);
        metadata = update;
      } else {
        log.debug("No update: None of the watch files have changed", metadata.path);
      }
    } catch (Exception e) {
      log.error("Caught exception while checking for updates", e);
    }
  }

  @VisibleForTesting
  void setUpdater(BiConsumer<T, T> updater) {
    this.updater = updater;
  }

  private void addWatchFile(File file) throws IllegalStateException {
    if (!watchFiles.containsKey(file.getAbsolutePath())) {
      com.xjeffrose.xio.config.RouteReloader.ConfigFileMetadata configMetaData = loadMetaData(file);
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
