import ch.qos.logback.classic.filter.ThresholdFilter

appender("CONSOLE", ConsoleAppender) {
  withJansi = true

  filter(ThresholdFilter) {
    level = DEBUG
  }
  encoder(PatternLayoutEncoder) {
    pattern = "%-4relative [%thread] %-5level %logger{30} - %msg%n"
    outputPatternAsHeader = false
  }
}

logger("io.netty.channel.DefaultChannelPipeline", DEBUG)
if (System.getProperty("DEBUG") != null) {
  root(DEBUG, ["CONSOLE"])
} else {
  root(WARN, ["CONSOLE"])
}
