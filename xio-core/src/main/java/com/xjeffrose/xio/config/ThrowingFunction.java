package com.xjeffrose.xio.config;

public interface ThrowingFunction<T, R> {
  R apply(T t) throws Exception;
}
