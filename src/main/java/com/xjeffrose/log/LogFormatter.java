package com.xjeffrose.log;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

class LogFormatter extends Formatter implements Glog.Formatter<LogRecord> {
  private Map<Level, Glog.Level> LEVEL_LABELS = new HashMap<Level, Glog.Level>();

  LogFormatter() {
    LEVEL_LABELS.put(Level.FINEST, Glog.Level.DEBUG);
    LEVEL_LABELS.put(Level.FINER, Glog.Level.DEBUG);
    LEVEL_LABELS.put(Level.FINE, Glog.Level.DEBUG);
    LEVEL_LABELS.put(Level.CONFIG, Glog.Level.INFO);
    LEVEL_LABELS.put(Level.INFO, Glog.Level.INFO);
    LEVEL_LABELS.put(Level.WARNING, Glog.Level.WARNING);
    LEVEL_LABELS.put(Level.SEVERE, Glog.Level.ERROR);
  }

  @Override
  public String format(final LogRecord record) {
    return Glog.formatRecord(this, record);
  }

  @Override
  public String getMessage(LogRecord record) {
    return formatMessage(record);
  }

  @Override
  public String getClassName(LogRecord record) {
    return record.getSourceClassName();
  }

  @Override
  public String getMethodName(LogRecord record) {
    return record.getSourceMethodName();
  }

  @Override
  public Glog.Level getLevel(LogRecord record) {
    return LEVEL_LABELS.get(record.getLevel());
  }

  @Override
  public long getTimeStamp(LogRecord record) {
    return record.getMillis();
  }

  @Override
  public long getThreadId(LogRecord record) {
    return record.getThreadID();
  }

  @Override
  public Throwable getThrowable(LogRecord record) {
    return record.getThrown();
  }
}

