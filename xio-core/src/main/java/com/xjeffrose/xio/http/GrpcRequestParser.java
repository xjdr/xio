package com.xjeffrose.xio.http;

import com.google.protobuf.InvalidProtocolBufferException;
import java.nio.ByteBuffer;

public interface GrpcRequestParser<T> {
  T parse(ByteBuffer buffer) throws InvalidProtocolBufferException;
}
