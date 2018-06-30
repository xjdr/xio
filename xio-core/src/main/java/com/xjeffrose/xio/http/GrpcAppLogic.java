package com.xjeffrose.xio.http;

import io.grpc.StatusException;

public interface GrpcAppLogic<GrpcRequest, GrpcResponse> {
  GrpcResponse apply(GrpcRequest request) throws StatusException;
}
