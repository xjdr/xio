package com.xjeffrose.xio.core;


import io.airlift.log.Logger;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.group.ChannelGroup;

public class ShutdownUtil {
  private static final Logger log = Logger.get(ShutdownUtil.class);

  public static void shutdownChannelFactory(ChannelFactory channelFactory,
                                            ExecutorService bossExecutor,
                                            ExecutorService workerExecutor,
                                            ChannelGroup allChannels) {
    // Close all channels
    if (allChannels != null) {
      closeChannels(allChannels);
    }

    // Shutdown the channel factory
    if (channelFactory != null) {
      channelFactory.shutdown();
    }

    // Stop boss threads
    if (bossExecutor != null) {
      shutdownExecutor(bossExecutor, "bossExecutor");
    }

    // Finally stop I/O workers
    if (workerExecutor != null) {
      shutdownExecutor(workerExecutor, "workerExecutor");
    }

    // Release any other resources netty might be holding onto via this channelFactory
    if (channelFactory != null) {
      channelFactory.releaseExternalResources();
    }
  }

  public static void closeChannels(ChannelGroup allChannels) {
    if (allChannels.size() > 0) {
      // TODO : allow an option here to control if we need to drain connections and wait instead of killing them all
      try {
        log.info("Closing %s open client connections", allChannels.size());
        if (!allChannels.close().await(5, TimeUnit.SECONDS)) {
          log.warn("Failed to close all open client connections");
        }
      } catch (InterruptedException e) {
        log.warn("Interrupted while closing client connections");
        Thread.currentThread().interrupt();
      }
    }
  }

  // TODO : make wait time configurable ?
  public static void shutdownExecutor(ExecutorService executor, final String name) {
    executor.shutdown();
    try {
      log.info("Waiting for %s to shutdown", name);
      if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
        log.warn("%s did not shutdown properly", name);
      }
    } catch (InterruptedException e) {
      log.warn("Interrupted while waiting for %s to shutdown", name);
      Thread.currentThread().interrupt();
    }
  }
}