package com.xjeffrose.xio.core;


import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class ShutdownUtil {


  public static void shutdownChannelFactory(EventLoopGroup group,
                                            ExecutorService bossExecutor,
                                            ExecutorService workerExecutor,
                                            ChannelGroup allChannels) {
    // Close all channels
    if (allChannels != null) {
      closeChannels(allChannels);
    }

    // Shutdown the channel factory
    if (group != null) {
      group.shutdownGracefully();
    }

    // Stop boss threads
    if (bossExecutor != null) {
      shutdownExecutor(bossExecutor, "bossExecutor");
    }

    // Finally stop I/O workers
    if (workerExecutor != null) {
      shutdownExecutor(workerExecutor, "workerExecutor");
    }

    // Release any other resources netty might be holding onto via this group
    if (group != null) {
      //TODO: Find netty4 equivalent (may not be nessisary with shutdown gracefully)
//      group.releaseExternalResources();
    }
  }

  public static void closeChannels(ChannelGroup allChannels) {
    if (allChannels.size() > 0) {
      // TODO : allow an option here to control if we need to drain connections and wait instead of killing them all
      try {
//        log.info("Closing %s open client connections", allChannels.size());
        if (!allChannels.close().await(5, TimeUnit.SECONDS)) {
//          log.warn("Failed to close all open client connections");
        }
      } catch (InterruptedException e) {
//        log.warn("Interrupted while closing client connections");
        Thread.currentThread().interrupt();
      }
    }
  }

  // TODO : make wait time configurable ?
  public static void shutdownExecutor(ExecutorService executor, final String name) {
    executor.shutdown();
    try {
//      log.info("Waiting for %s to shutdown", name);
      if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
//        log.warn("%s did not shutdown properly", name);
      }
    } catch (InterruptedException e) {
//      log.warn("Interrupted while waiting for %s to shutdown", name);
      Thread.currentThread().interrupt();
    }
  }
}
