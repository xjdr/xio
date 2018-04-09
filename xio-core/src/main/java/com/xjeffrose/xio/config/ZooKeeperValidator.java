package com.xjeffrose.xio.config;

import com.typesafe.config.Config;
import com.xjeffrose.xio.storage.ZooKeeperReadProvider;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ZooKeeperValidator {

  private final ZooKeeperReadProvider zkReader;
  private final Ruleset rules;
  private final Config config;

  public ZooKeeperValidator(ZooKeeperReadProvider zkReader, Ruleset rules, Config config) {
    this.zkReader = zkReader;
    this.rules = rules;
    this.config = config;
  }

  public void validate() {
    Ruleset stored = new Ruleset(config);
    stored.read(zkReader);
    if (stored.equals(rules)) {
      log.info("stored rules match rules in memory");
    } else {
      log.error("stored rules do not match rules in memory");
    }
  }
}
