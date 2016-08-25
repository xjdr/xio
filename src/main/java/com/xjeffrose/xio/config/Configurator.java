package com.xjeffrose.xio.config;

import com.typesafe.config.Config;
import com.xjeffrose.xio.config.thrift.ConfigurationService;
import com.xjeffrose.xio.config.thrift.IpRule;
import com.xjeffrose.xio.config.thrift.Result;
import com.xjeffrose.xio.config.thrift.RuleType;
import com.xjeffrose.xio.marshall.ThriftMarshaller;
import com.xjeffrose.xio.marshall.ThriftUnmarshaller;
import com.xjeffrose.xio.marshall.thrift.Http1Rule;
import com.xjeffrose.xio.storage.ZooKeeperReadProvider;
import com.xjeffrose.xio.storage.ZooKeeperWriteProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryOneTime;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TServer.Args;
import org.apache.thrift.server.TSimpleServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.transport.TTransportException;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.TimerTask;
import java.util.Timer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

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
    timer.cancel();
    server.stop();
  }

  public void run() {
    log.info("Starting up!");
    try {
      serverTransport = new TServerSocket(bindAddress);
      server = new TSimpleServer(new Args(serverTransport).processor(processor));
      server.serve(); // blocks until stop() is called.
      // TODO(CK): handle remaining workload
    } catch (TTransportException e) {
      log.error("Couldn't start Configurator {}", this, e);
    }
  }

  public static Configurator build(String zkCluster, Config config) {
    CuratorFramework client = CuratorFrameworkFactory.newClient(zkCluster, new RetryOneTime(2000));
    client.start();
    ZooKeeperWriteProvider zkWriter = new ZooKeeperWriteProvider(new ThriftMarshaller(), client);
    ZooKeeperReadProvider zkReader = new ZooKeeperReadProvider(new ThriftUnmarshaller(), client);

    Ruleset rules = new Ruleset(config);
    rules.read(zkReader);
    ZooKeeperUpdateHandler zkUpdater = new ZooKeeperUpdateHandler(zkWriter, rules);
    ZooKeeperValidator zkValidator = new ZooKeeperValidator(zkReader, rules, config);
    Duration updateInterval = Duration.ofSeconds(5);
    InetSocketAddress serverAddress = new InetSocketAddress("localhost", 9999);
    Configurator server = new Configurator(zkUpdater, updateInterval, serverAddress, rules, zkValidator);
    return server;
  }

}
