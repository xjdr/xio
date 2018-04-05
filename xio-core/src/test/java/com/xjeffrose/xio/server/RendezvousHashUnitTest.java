package com.xjeffrose.xio.server;

import static org.junit.Assert.assertEquals;

import com.google.common.collect.ImmutableMap;
import com.google.common.hash.Funnels;
import com.xjeffrose.xio.core.Constants;
import io.netty.util.internal.PlatformDependent;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.Assert;
import org.junit.Test;

public class RendezvousHashUnitTest extends Assert {
  @Test
  public void get() throws Exception {
    List<String> nodeList = new ArrayList<>();
    Map<String, List<String>> mm = PlatformDependent.newConcurrentHashMap();
    for (int i = 0; i < 100; i++) {
      nodeList.add(("Host" + i));
      mm.put(("Host" + i), new ArrayList<>());
    }

    RendezvousHash<CharSequence> rendezvousHash =
        new RendezvousHash<>(Funnels.stringFunnel(Charset.defaultCharset()), nodeList);
    Random r = new Random();
    for (int i = 0; i < 100000; i++) {
      String thing = (Integer.toString(r.nextInt(123456789)));
      List<CharSequence> hosts = rendezvousHash.get(thing.getBytes(), 3);
      hosts.forEach(
          xs -> {
            mm.get(xs).add(thing);
          });
    }

    List<Integer> xx = new ArrayList<>();
    mm.keySet()
        .forEach(
            xs -> {
              xx.add(mm.get(xs).size());
            });

    Double xd = xx.stream().mapToInt(x -> x).average().orElse(-1);
    assertEquals(3000, xd.intValue());
  }

  @Test
  public void simpleGet() {
    Map<String, String> map =
        new ImmutableMap.Builder<String, String>()
            .put("a", "1")
            .put("b", "2")
            .put("c", "3")
            .put("d", "4")
            .put("e", "5")
            .build();
    RendezvousHash<CharSequence> hasher =
        new RendezvousHash<>(Funnels.stringFunnel(Constants.DEFAULT_CHARSET), map.keySet());
    String k1 = "foo";
    String k2 = "bar";
    String k3 = "baz";

    assertEquals(hasher.getOne(k1.getBytes()), hasher.getOne(k1.getBytes()));
    assertEquals(hasher.getOne(k2.getBytes()), hasher.getOne(k2.getBytes()));
    assertEquals(hasher.getOne(k3.getBytes()), hasher.getOne(k3.getBytes()));
    String k4 = "biz";
    assertEquals(hasher.getOne(k4.getBytes()), hasher.getOne(k4.getBytes()));

    System.out.println(hasher.getOne(k1.getBytes()));
    System.out.println(hasher.getOne(k2.getBytes()));
    System.out.println(hasher.getOne(k3.getBytes()));
    System.out.println(hasher.getOne(k4.getBytes()));

    System.out.println(hasher.getOne(k1.getBytes()));
    System.out.println(hasher.getOne(k2.getBytes()));
    System.out.println(hasher.getOne(k3.getBytes()));
    System.out.println(hasher.getOne(k4.getBytes()));

    assertNotEquals(hasher.getOne(k1.getBytes()), hasher.getOne(k4.getBytes()));
  }
}
