package com.xjeffrose.xio.grpc;

import com.xjeffrose.xio.http.GrpcRequestHandler;

public class GrpcRoute {
  public final GrpcService service;
  public final String methodName;
  public final GrpcRequestHandler handler;

  /**
   * A gRPC route that can be used with a PipelineRequestHandler.
   *
   * @param service the service creating this route.
   * @param methodName the name of the gRPC method. Found in the `.proto` file, in the service
   *     definition, after the `rpc` keyword.
   * @param handler a subclass of GrpcRequestHandler that is specified for a specific Request and
   *     Response types and has the business logic for the route.
   */
  public GrpcRoute(GrpcService service, String methodName, GrpcRequestHandler handler) {
    this.service = service;
    this.methodName = methodName;
    this.handler = handler;
  }

  /** Convenience method for build the path based on the gRPC spec. */
  public String buildPath() {
    return "/" + service.getPackageName() + "." + service.getServiceName() + "/" + methodName;
  }

  /**
   * Convenience method for build the OU (or our version of a service name) based on the gRPC spec.
   */
  public String buildOu() {
    return service.getPackageName() + "." + service.getServiceName();
  }
}
