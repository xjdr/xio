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

logger("com.xjeffrose.xio.core.NullZkClient", OFF)

if (System.getProperty("DEBUG") != null) {
  root(DEBUG, ["CONSOLE"])
} else if (System.getProperty("COVERAGE") != null) {
  root(DEBUG, ["DEVNULL"])
} else {
  root(WARN, ["CONSOLE"])
}
