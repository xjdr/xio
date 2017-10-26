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
root(WARN, ["CONSOLE"])
//TODO(CK): figure out how to set this from the command line
//root(DEBUG, ["CONSOLE"])
