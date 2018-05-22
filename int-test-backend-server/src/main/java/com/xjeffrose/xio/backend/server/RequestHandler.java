package com.xjeffrose.xio.backend.server;

public interface RequestHandler {
  ResponseBuilder request(ResponseBuilder responseBuilder) throws Exception;
}
