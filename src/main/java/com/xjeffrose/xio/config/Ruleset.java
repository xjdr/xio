package com.xjeffrose.xio.config;

import com.typesafe.config.Config;
import com.xjeffrose.xio.storage.ReadProvider;
import com.xjeffrose.xio.storage.WriteProvider;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class Ruleset {

  static public class Markable<T> {
    private final T data;
    private boolean marked = false;

    Markable(T data) {
      this.data = data;
    }

    public T mutate() {
      marked = true;
      return data;
    }

    public T get() {
      return data;
    }

    public boolean marked() {
      return marked;
    }

    public void clearMark() {
      marked = false;
    }
  }

  private final Markable<IpAddressDeterministicRuleEngineConfig> ipRules = new Markable<>(new IpAddressDeterministicRuleEngineConfig());
  private final Markable<Http1DeterministicRuleEngineConfig> http1Rules = new Markable<>(new Http1DeterministicRuleEngineConfig());

  private final String ipFilterPath;
  private final String http1FilterPath;

  public Ruleset(Config config) {
    ipFilterPath = config.getString("ipFilter.path");
    http1FilterPath = config.getString("http1Filter.path");

  }

  public long write(WriteProvider writer) {
    long recordsWritten = 0;

    if (ipRules.marked()) {
      writer.write(ipFilterPath, ipRules.get());
      recordsWritten += ipRules.get().size();
      ipRules.clearMark();
    }

    if (http1Rules.marked()) {
      writer.write(http1FilterPath, http1Rules.get());
      recordsWritten += http1Rules.get().size();
      http1Rules.clearMark();
    }

    return recordsWritten;
  }

  public void read(ReadProvider reader) {
    reader.read(ipFilterPath, ipRules.get());
    reader.read(http1FilterPath, http1Rules.get());
  }

  public IpAddressDeterministicRuleEngineConfig mutateIpRules() {
    return ipRules.mutate();
  }

  public Http1DeterministicRuleEngineConfig mutateHttpRules() {
    return http1Rules.mutate();
  }
}
