package com.xjeffrose.xio.config;

import com.xjeffrose.xio.storage.ZooKeeperReadProvider;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ZooKeeperValidator {

  private final ZooKeeperReadProvider zkReader;
  private final Ruleset rules;

  public ZooKeeperValidator(ZooKeeperReadProvider zkReader, Ruleset rules) {
    this.zkReader = zkReader;
    this.rules = rules;
  }

  public void validate() {
    Ruleset stored = new Ruleset();
    stored.read(zkReader);
    if (stored.equals(rules)) {
      log.info("stored rules match rules in memory");
    } else {
      log.error("stored rules do not match rules in memory");
    }
  }

}
