package com.xjeffrose.xio.tracing;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class XioTracingTest extends Assert {

  private XioTracing subject;
  private Config validParametersConfig;
  private Config invalidParametersConfig;

  @Before
  public void setUp() {
    validParametersConfig = ConfigFactory.load().getConfig("xio.validZipkinParameters");
    invalidParametersConfig = ConfigFactory.load().getConfig("xio.invalidZipkinParameters");
  }

  @Test
  public void testValidParametersConfig() {
    // This when the zipkinurl is non-empty AND the sampling rate is > 0.0f
    subject = new XioTracing(validParametersConfig);
    assertTrue(subject.newClientHandler(true) != null);
    assertTrue(subject.newClientHandler(false) != null);
    assertTrue(subject.newServerHandler(true) != null);
    assertTrue(subject.newServerHandler(false) != null);
  }

  @Test
  public void testInvalidParametersConfig() {
    // This when the zipkinurl is empty OR the sampling rate is <= 0.0f
    subject = new XioTracing(invalidParametersConfig);
    assertTrue(subject.newClientHandler(true) == null);
    assertTrue(subject.newClientHandler(false) == null);
    assertTrue(subject.newServerHandler(true) == null);
    assertTrue(subject.newServerHandler(false) == null);
  }
}
