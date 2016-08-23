package com.xjeffrose.xio.config;

import com.typesafe.config.Config;
import com.xjeffrose.xio.config.thrift.ConfigurationService;
import com.xjeffrose.xio.config.thrift.IpRule;
import com.xjeffrose.xio.config.thrift.Result;
import com.xjeffrose.xio.config.thrift.RuleType;
import com.xjeffrose.xio.marshall.ThriftMarshaller;
import com.xjeffrose.xio.marshall.ThriftUnmarshaller;
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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
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
  private final Map<InetAddress, RuleType> rules = new HashMap<>();
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

  public Configurator(UpdateHandler storage, Duration updateInterval, InetSocketAddress bindAddress, ZooKeeperValidator zkValidator) {
    this.storage = storage;
    this.updateInterval = updateInterval;
    this.bindAddress = bindAddress;
    this.zkValidator = zkValidator;
    serverThread = new Thread(this, "Configurator server thread");
    service = new ConfigurationService.Iface() {
      @Override
      public Result addIpRule(IpRule ipRule) throws org.apache.thrift.TException {
        log.info("add {}", ipRule);
        try {
          InetAddress address = InetAddress.getByAddress(ipRule.getIpAddress());
          log.debug("address {}", address.getHostAddress());
          RuleType ruleType = rules.get(address);
          if (ruleType != null && ruleType.equals(ipRule.getRuleType())) {
            return new Result(false, "address " + address.getHostAddress() + " already on " + ruleType);
          } else {
            workLoad.put(UpdateMessage.addIpRule(address, ipRule.getRuleType()));
            rules.put(address, ipRule.getRuleType());
            log.debug("rules {}", rules);
          }
        } catch(UnknownHostException | InterruptedException e) {
          log.error("addIpRule couldn't add {}", ipRule, e);
          return new Result(false, e.getMessage());
        }

        return new Result(true, "");
      }

      @Override
      public Result removeIpRule(IpRule ipRule) throws org.apache.thrift.TException {
        log.info("remove {}", ipRule);
        try {
          InetAddress address = InetAddress.getByAddress(ipRule.getIpAddress());
          log.debug("address {}", address.getHostAddress());
          if (!rules.containsKey(address)) {
            return new Result(false, "nothing to remove for address " + address.getHostAddress());
          } else {
            workLoad.put(UpdateMessage.removeIpRule(address));
            rules.remove(address);
          }
        } catch(UnknownHostException | InterruptedException e) {
          log.error("addIpRule couldn't add {}", ipRule, e);
          return new Result(false, e.getMessage());
        }
        return new Result(true, "");
      }
    };

    processor = new ConfigurationService.Processor<>(service);
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
    Configurator server = new Configurator(zkUpdater, updateInterval, serverAddress, zkValidator);
    return server;
  }

}
