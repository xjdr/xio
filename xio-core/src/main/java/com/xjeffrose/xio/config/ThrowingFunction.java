package com.xjeffrose.xio.config;

import java.util.function.Function;

public interface ThrowingFunction<T, R> {
  R apply(T t) throws Exception;
}
