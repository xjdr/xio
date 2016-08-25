package com.xjeffrose.xio.core;

import com.typesafe.config.Config;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.BoundedExponentialBackoffRetry;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.retry.RetryForever;
import org.apache.curator.retry.RetryNTimes;
import org.apache.curator.retry.RetryOneTime;
import org.apache.curator.retry.RetryUntilElapsed;

import java.util.Arrays;

public class ZooKeeperClientFactory {
  enum ClientRetryPolicy {
    BoundedExponentialBackoffRetry {
      @Override
      RetryPolicy build(Config config) {
        return new BoundedExponentialBackoffRetry(getMillis(config, "baseSleepDuration"), getMillis(config, "maxSleepDuration"), config.getInt("maxRetries"));
      }
    },
    ExponentialBackoffRetry {
      @Override
      RetryPolicy build(Config config) {
        return new ExponentialBackoffRetry(getMillis(config, "baseSleepDuration"), config.getInt("maxRetries"));
      }
    },
    RetryForever {
      @Override
      RetryPolicy build(Config config) {
        return new RetryForever(getMillis(config, "sleepDuration"));
      }
    },
    RetryNTimes {
      @Override
      RetryPolicy build(Config config) {
        return new RetryNTimes(config.getInt("n"), getMillis(config, "sleepDuration"));
      }
    },
    RetryOneTime {
      @Override
      RetryPolicy build(Config config) {
        return new RetryOneTime(getMillis(config, "sleepDuration"));
      }
    },
    RetryUntilElapsed {
      @Override
      RetryPolicy build(Config config) {
        return new RetryUntilElapsed(getMillis(config, "maxElapsedDuration"), getMillis(config, "sleepDuration"));
      }
    };

    int getMillis(Config config, String path) {
      return (int)config.getDuration(path).toMillis();
    }
    abstract RetryPolicy build(Config config);
  }

  Config config;

  public ZooKeeperClientFactory(Config config) {
    this.config = config;
  }

  public CuratorFramework newClient() {
    Config retry = config.getConfig("client.retry");
    try {
      ClientRetryPolicy policy = ClientRetryPolicy.valueOf(retry.getString("policy"));
      return CuratorFrameworkFactory.newClient(config.getString("cluster"), policy.build(retry.getConfig(policy.name())));
    } catch (IllegalArgumentException e) {
      throw new RuntimeException("zookeeper.client.retry.policy must be one of " + Arrays.asList(ClientRetryPolicy.values()));
    }
  }

}
