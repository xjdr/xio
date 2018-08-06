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
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RouteReloader<T> {

  private final ScheduledExecutorService executor;
  private final ThrowingFunction<String, T> factory;
  private BiConsumer<T, T> updater;
  private RouteMeta<T> metadata;
  private Map<String, RouteConfigFileMetadata> watchFiles =
      new HashMap<String, RouteConfigFileMetadata>();

  public RouteReloader(ScheduledExecutorService executor, ThrowingFunction<String, T> factory) {
    this.executor = executor;
    this.factory = factory;
    this.updater = null;
  }

  public T init(String file) {
    metadata = load(file);
    addWatchFile(new File(file));
    return metadata.value;
  }

  public void start(BiConsumer<T, T> updater) {
    checkNotNull(updater, "updater cannot be null");
    checkNotNull(metadata, "init must be called before start");
    checkState(this.updater == null, "start cannot be called more than once");
    setUpdater(updater);
    executor.scheduleWithFixedDelay(this::checkForUpdates, 2000, 2000, TimeUnit.MILLISECONDS);
  }

  private RouteMeta<T> load(String filePath) {
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
      return new RouteMeta<>(value, file.getAbsolutePath(), file.lastModified(), digest.digest());
    } catch (Exception e) {
      throw new IllegalStateException("Couldn't load config from file '" + filePath + "'", e);
    }
  }

  private boolean shouldPerformUpdate(
      RouteConfigFileMetadata current, RouteConfigFileMetadata previous) {
    return (current.lastModified > previous.lastModified
        && !MessageDigest.isEqual(current.digest, previous.digest));
  }

  private RouteConfigFileMetadata loadMetaData(File configFile) throws IllegalStateException {
    final MessageDigest digest;
    try {
      digest = MessageDigest.getInstance("SHA-256");
    } catch (Exception e) {
      throw new IllegalStateException(
          "Couldn't load config from file '" + configFile.getAbsolutePath() + "'", e);
    }

    try {
      digest.update(Files.readAllBytes(Paths.get(configFile.getAbsolutePath())));
      return new RouteConfigFileMetadata(
          configFile.getAbsolutePath(), configFile.lastModified(), digest.digest());
    } catch (Exception e) {
      throw new IllegalStateException(
          "Couldn't load config from file '" + configFile.getAbsolutePath() + "'", e);
    }
  }

  private void updateChangedWatchFiles(List<RouteConfigFileMetadata> toUpdateList) {
    toUpdateList.forEach(update -> watchFiles.put(update.path, update));
  }

  private List<RouteConfigFileMetadata> getChangedFiles() {
    try {
      Set<String> keys = new HashSet<String>(watchFiles.keySet());
      List<RouteConfigFileMetadata> toUpdateList =
          keys.stream()
              .map(filePath -> loadMetaData(new File(filePath)))
              .filter(
                  currentMetaData ->
                      shouldPerformUpdate(currentMetaData, watchFiles.get(currentMetaData.path)))
              .collect(Collectors.toList());

      return toUpdateList;
    } catch (Exception e) {
      log.error("Caught exception while checking for if watch files have changed", e);
    }
    return new ArrayList<RouteConfigFileMetadata>();
  }

  @VisibleForTesting
  void checkForUpdates() {
    try {
      // check to see if any of the specific watch files have changed
      List<RouteConfigFileMetadata> toUpdateList = getChangedFiles();
      if (!toUpdateList.isEmpty()) {
        updateChangedWatchFiles(toUpdateList);
        // suck up the original file which includes all the sub files
        RouteMeta<T> update = load(metadata.path);
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
      RouteConfigFileMetadata configMetaData = loadMetaData(file);
      watchFiles.put(file.getAbsolutePath(), configMetaData);
    } else {
      log.error("Attempted to add duplicate watch file: " + file.getAbsolutePath());
    }
  }

  private static class RouteMeta<T> {
    final T value;
    final String path;
    final long lastModified;
    final byte[] digest;

    RouteMeta(T value, String path, long lastModified, byte[] digest) {
      this.value = value;
      this.path = path;
      this.lastModified = lastModified;
      this.digest = digest;
    }
  }

  private static class RouteConfigFileMetadata {
    final String path;
    final long lastModified;
    final byte[] digest;

    RouteConfigFileMetadata(String path, long lastModified, byte[] digest) {
      this.path = path;
      this.lastModified = lastModified;
      this.digest = digest;
    }
  }
}
