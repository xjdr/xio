package com.xjeffrose.xio.core;

import io.netty.util.AttributeKey;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class Constants {

  public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

  public static final AttributeKey<Boolean> HARD_RATE_LIMITED =
      AttributeKey.valueOf("hard_rate_limited");
  public static final AttributeKey<Boolean> SOFT_RATE_LIMITED =
      AttributeKey.valueOf("soft_rate_limited");
  public static final AttributeKey<Boolean> IP_WHITE_LIST = AttributeKey.valueOf("IpWhiteList");
  public static final AttributeKey<Boolean> IP_BLACK_LIST = AttributeKey.valueOf("IpBlackList");

  private Constants() {}
}
