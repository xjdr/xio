import ch.qos.logback.classic.filter.ThresholdFilter
import ch.qos.logback.classic.PatternLayout

// make changes for dev appender here
appender("DEV-CONSOLE", ConsoleAppender) {
  withJansi = true

  filter(ThresholdFilter) {
    level = DEBUG
  }
  encoder(PatternLayoutEncoder) {
    pattern = "%-4relative [%thread] %-5level %logger{30} - %msg%n"
    outputPatternAsHeader = false
  }
}

logger("com.xjeffrose.xio.config.ConfigReloader", OFF)
logger("com.xjeffrose.xio.SSL.XioTrustManagerFactory", OFF)
logger("com.xjeffrose.xio.core.NullZkClient", OFF)
logger("io.netty.channel.DefaultChannelPipeline", DEBUG)
logger("io.netty.util.internal.NativeLibraryLoader", ERROR)
logger("io.netty.handler.ssl.CipherSuiteConverter", OFF)

root(ALL, ["DEV-CONSOLE"])
