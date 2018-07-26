package com.xjeffrose.xio.grpc;

import java.util.List;

/**
 * An api for a gRPC service
 *
 * <p>packageName: the name of the gRPC package. Found in the `.proto` file, at the top of the file,
 * after the `package` keyword. serviceName: the name of the gRPC service. Found in the `.proto`
 * file, in the service definition, after the `service` keyword. routes: the list of routes for each
 * method. See GrpcRoute.java for more info.
 */
public interface GrpcService {
  String getPackageName();

  String getServiceName();

  List<GrpcRoute> getRoutes();
}
