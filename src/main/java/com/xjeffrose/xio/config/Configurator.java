package com.xjeffrose.xio.config;

import com.typesafe.config.Config;
import com.xjeffrose.xio.config.thrift.ConfigurationService;
import com.xjeffrose.xio.config.thrift.IpRule;
import com.xjeffrose.xio.config.thrift.Result;
import com.xjeffrose.xio.config.thrift.RuleType;
import com.xjeffrose.xio.core.ZooKeeperClientFactory;
import com.xjeffrose.xio.marshall.ThriftMarshaller;
import com.xjeffrose.xio.marshall.ThriftUnmarshaller;
import com.xjeffrose.xio.marshall.thrift.Http1Rule;
import com.xjeffrose.xio.storage.ZooKeeperReadProvider;
import com.xjeffrose.xio.storage.ZooKeeperWriteProvider;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TServer.Args;
import org.apache.thrift.server.TSimpleServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.transport.TTransportException;

@Slf4j
public class Configurator implements Runnable {
  private final UpdateHandler storage;
  private final Duration updateInterval;
  private final InetSocketAddress bindAddress;
  private final ZooKeeperValidator zkValidator;
  private final Thread serverThread;
  private final ConfigurationService.Iface service;
  private final ConfigurationService.Processor<ConfigurationService.Iface> processor;
  private TServerTransport serverTransport;
  private TServer server;
  private final IpRules ipRules;
  private final Http1Rules http1Rules;
  private final BlockingQueue<UpdateMessage> workLoad = new LinkedBlockingQueue<>();
  private final Timer timer = new Timer("Configurator update thread", true);
  private final TimerTask timerTask = new TimerTask() {
    @Override
    public void run() {
      writeToStorage();
    }
  };
  private int storageRuns = 0;
  private int validationInterval = 2;

  private void writeToStorage() {
    storageRuns++;
    log.info("writeToStorage - started");
    ArrayList<UpdateMessage> messages = new ArrayList<>();
    workLoad.drainTo(messages);
    messages.forEach((m) -> m.process(storage));
    long recordsWritten = storage.commit();
    log.info("writeToStorage - finished {} updates written {} records written", messages.size(), recordsWritten);
    if (storageRuns % validationInterval == 0) {
      zkValidator.validate();
    }
  }

  private ConfigurationService.Iface newService() {
    return new ConfigurationService.Iface() {
      @Override
      public Result addIpRule(IpRule ipRule, RuleType ruleType) throws org.apache.thrift.TException {
        log.info("addIpRule {} {}", ipRule, ruleType);
        return ipRules.add(ipRule, ruleType, workLoad);
      }

      @Override
      public Result removeIpRule(IpRule ipRule) throws org.apache.thrift.TException {
        log.info("removeIpRule {}", ipRule);
        return ipRules.remove(ipRule, workLoad);
      }


      @Override
      public Result addHttp1Rule(Http1Rule http1Rule, RuleType ruleType) throws org.apache.thrift.TException {
        log.info("addHttp1Rule {} {}", http1Rule, ruleType);
        return http1Rules.add(http1Rule, ruleType, workLoad);
      }

      @Override
      public Result removeHttp1Rule(Http1Rule http1Rule) throws org.apache.thrift.TException {
        log.info("removeHttp1Rule {}", http1Rule);
        return http1Rules.remove(http1Rule, workLoad);
      }

    };
  }

  public Configurator(UpdateHandler storage, Duration updateInterval, InetSocketAddress bindAddress, Ruleset existing, ZooKeeperValidator zkValidator) {
    this.storage = storage;
    this.updateInterval = updateInterval;
    this.bindAddress = bindAddress;
    this.zkValidator = zkValidator;
    serverThread = new Thread(this, "Configurator server thread");
    service = newService();
    processor = new ConfigurationService.Processor<>(service);
    ipRules = new IpRules(existing);
    http1Rules = new Http1Rules(existing);
  }

  public void start() {
    timer.scheduleAtFixedRate(timerTask, 0, updateInterval.toMillis());
    serverThread.start();
  }

  public void close() {
    timerTask.cancel(); // cancels the task in flight
    timer.cancel(); // cancels the timer thread
    server.stop(); // stops the thrift server
  }

  public void run() {
    log.info("Starting up!");
    try {
      serverTransport = new TServerSocket(bindAddress);
      server = new TSimpleServer(new Args(serverTransport).processor(processor));
      server.serve(); // blocks until stop() is called.
      // timer and timer task should be stopped at this point
      writeToStorage();
    } catch (TTransportException e) {
      log.error("Couldn't start Configurator {}", this, e);
    }
  }

  public static class NullConfigurator extends Configurator {
    NullConfigurator() {
      super(null, null, null, new Ruleset(), null);
    }

    @Override
    public void start() {
    }
    @Override
    public void close() {
    }

  }

  public static Configurator build(Config config) {
    Config configurationUpdateServer = config.getConfig("configurationUpdateServer");
    if (configurationUpdateServer.getBoolean("enabled") == false) {
      return new NullConfigurator();
    }
    CuratorFramework client = new ZooKeeperClientFactory(config.getConfig("zookeeper")).newClient();
    client.start();
    ZooKeeperWriteProvider zkWriter = new ZooKeeperWriteProvider(new ThriftMarshaller(), client);
    ZooKeeperReadProvider zkReader = new ZooKeeperReadProvider(new ThriftUnmarshaller(), client);

    Config configurationManager = config.getConfig("configurationManager");
    Ruleset rules = new Ruleset(configurationManager);
    rules.read(zkReader);
    ZooKeeperUpdateHandler zkUpdater = new ZooKeeperUpdateHandler(zkWriter, rules);
    ZooKeeperValidator zkValidator = new ZooKeeperValidator(zkReader, rules, configurationManager);

    Duration writeInterval = configurationUpdateServer.getDuration("writeInterval");
    InetSocketAddress serverAddress = new InetSocketAddress(configurationUpdateServer.getString("bindIp"), configurationUpdateServer.getInt("bindPort"));
    Configurator server = new Configurator(zkUpdater, writeInterval, serverAddress, rules, zkValidator);
    return server;
  }

}
