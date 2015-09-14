package com.xjeffrose.xio.fixtures;

import com.xjeffrose.xio.processor.XioProcessor;
import com.xjeffrose.xio.processor.XioProcessorFactory;

public class XioTestProcessorFactory implements XioProcessorFactory {
  @Override
  public XioProcessor getProcessor() {
    return new XioTestProcessor();
  }
}
