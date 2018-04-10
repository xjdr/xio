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
    assertNotNull(subject.newClientHandler(true));
    assertNotNull(subject.newClientHandler(false));
    assertNotNull(subject.newServerHandler(true));
    assertNotNull(subject.newServerHandler(false));
  }

  @Test
  public void testInvalidParametersConfig() {
    // This when the zipkinurl is empty OR the sampling rate is <= 0.0f
    subject = new XioTracing(invalidParametersConfig);
    assertNull(subject.newClientHandler(true));
    assertNull(subject.newClientHandler(false));
    assertNull(subject.newServerHandler(true));
    assertNull(subject.newServerHandler(false));
  }
}
