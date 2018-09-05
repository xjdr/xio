package com.xjeffrose.xio.tracing;

import com.typesafe.config.ConfigFactory;
import com.xjeffrose.xio.config.TracingConfig;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class XioTracingTest extends Assert {

  private XioTracing subject;
  private TracingConfig validParametersConfig;
  private TracingConfig invalidParametersConfig;

  @Before
  public void setUp() {
    validParametersConfig =
        new TracingConfig(
            "xio-tracing-test",
            ConfigFactory.load().getConfig("xio.validZipkinParameters.settings.tracing"));
    invalidParametersConfig =
        new TracingConfig(
            "xio-tracing-test",
            ConfigFactory.load().getConfig("xio.invalidZipkinParameters.settings.tracing"));
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
