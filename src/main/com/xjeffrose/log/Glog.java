package com.xjeffrose.log;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

class Glog {

  public enum Level {
    UNKNOWN('U'),
    DEBUG('D'),
    INFO('I'),
    WARNING('W'),
    ERROR('E'),
    FATAL('F');
    char label;
    private Level(char label) {
      this.label = label;
    }
  }

  public interface Formatter<T> {
    String getMessage(T record);
    String getClassName(T record);
    String getMethodName(T record);
    Level getLevel(T record);
    long getTimeStamp(T record);
    long getThreadId(T record);
    Throwable getThrowable(T record);
  }

  private static final int BASE_MESSAGE_LENGTH = 30;

  public static <T> String formatRecord(Formatter<T> formatter, T record) {
    String message = formatter.getMessage(record);
    int messageLength = BASE_MESSAGE_LENGTH + 2 + message.length();
    String className = formatter.getClassName(record);
    String methodName = null;
    if (className != null) {
      messageLength += className.length();
      methodName = formatter.getMethodName(record);
      if (methodName != null) {
        messageLength += 1;  // Period between class and method.
        messageLength += methodName.length();
      }
    }

    StringBuilder sb = new StringBuilder(messageLength)
        // TODO implement Lambda for checking nonNull logs
        //.append( (T r) -> formatter.getLevel(r)  != null ? formatter.getLevel(r) : Level.UNKNOWN.label)
        .append(formatter.getLevel(record).label)
        .append(LocalDateTime.ofInstant(Instant.ofEpochMilli(formatter.getTimeStamp(record)),
            ZoneOffset.UTC).format(DateTimeFormatter.ofPattern(" MMdd HH:mm:ss.SSS")))
        .append(formatter.getTimeStamp(record))
        .append(" THREAD")
        .append(formatter.getThreadId(record));
    if (className != null) {
      sb.append(' ').append(className);
      if (methodName != null) {
        sb.append('.').append(methodName);
      }
    }

    sb.append(": ").append(message);
    Throwable throwable = formatter.getThrowable(record);
    if (throwable != null) {
      sb.append('\n').append(throwable.getStackTrace());
    }

    return sb.append('\n').toString();
  }
}
