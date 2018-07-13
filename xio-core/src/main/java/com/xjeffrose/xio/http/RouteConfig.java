package com.xjeffrose.xio.http;

import com.google.common.base.Preconditions;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigOrigin;
import io.netty.handler.codec.http.HttpMethod;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@Getter
public class RouteConfig {
  static enum Method {
    CONNECT(HttpMethod.CONNECT),
    DELETE(HttpMethod.DELETE),
    GET(HttpMethod.GET),
    HEAD(HttpMethod.HEAD),
    OPTIONS(HttpMethod.OPTIONS),
    PATCH(HttpMethod.PATCH),
    POST(HttpMethod.POST),
    PUT(HttpMethod.PUT),
    TRACE(HttpMethod.TRACE);

    private final HttpMethod httpMethod;

    Method(HttpMethod httpMethod) {
      this.httpMethod = httpMethod;
    }

    public HttpMethod method() {
      return httpMethod;
    }
  }

  private final List<HttpMethod> methods;
  private final String host; // just the host part of the authority
  private final String path;
  private final String permissionNeeded;

  private static List<HttpMethod> convert(List<Method> methods) {
    return methods.stream().map(Method::method).collect(Collectors.toList());
  }

  protected static String location(Config config, String key) {
    ConfigOrigin origin = config.getValue(key).origin();
    return origin.description() + ":" + origin.lineNumber();
  }

  protected static String notEmpty(Config config, String key) {
    String value = config.getString(key);
    Preconditions.checkArgument(
        !value.isEmpty(), "%s %s must not be empty", location(config, key), key);
    return value;
  }

  protected static String startsWith(Config config, String key, String startsWith) {
    String value = config.getString(key);
    Preconditions.checkArgument(
        value.startsWith(startsWith),
        "%s %s '%s' must start with '%s'",
        location(config, key),
        key,
        value,
        startsWith);
    return value;
  }

  protected static String endsWith(Config config, String key, String endsWith) {
    String value = config.getString(key);
    Preconditions.checkArgument(
        value.endsWith(endsWith),
        "%s %s '%s' must end with '%s'",
        location(config, key),
        key,
        value,
        endsWith);
    return value;
  }

  protected static void ensureNotEmpty(String name, String value) {
    Preconditions.checkArgument(!value.isEmpty(), "%s '%s' must not be empty", name, value);
  }

  protected static void ensureStartsWith(String name, String value, String startsWith) {
    Preconditions.checkArgument(value.startsWith(startsWith), "%s '%s' must start with '%s'", name, value, startsWith);
  }

  protected static void ensureEndsWith(String name, String value, String endsWith) {
    Preconditions.checkArgument(value.endsWith(endsWith), "%s '%s' must end with '%s'", name, value, endsWith);
  }

  /*
      https://docs.oracle.com/javase/8/docs/api/java/util/function/Predicate.html
  https://github.com/google/guava/wiki/FunctionalExplained
  http://google.github.io/guava/releases/snapshot/api/docs/com/google/common/base/Predicates.html
  http://google.github.io/guava/releases/snapshot/api/docs/com/google/common/base/Preconditions.html
  https://github.com/google/guava/wiki/PreconditionsExplained

    class ConfigPredicate {
      String msg;
    }
    protected static String validate(Config config, String key, Predicate<String> pred, String msg) {
      String value = config.getString(key);
      Preconditions.checkArgument(pred.test(value), "%s %s '%s' %s", location(config, key), key, value, msg);
      return value;
    }
    */

  // TODO(br): find a way to combine preconditions
  public RouteConfig(Config config) {
    this.methods = convert(config.getEnumList(Method.class, "methods"));
    this.host = config.getString("host");
    this.path = notEmpty(config, "path");
    //    Preconditions.checkArgument(!path.isEmpty(), "%s path must not be empty",
    // config.getValue("path").origin());
    startsWith(config, "path", "/");
    // Preconditions.checkArgument(path.startsWith("/"), "%s path '%s' must start with '/'",
    // location(config, "path"), path);

    this.permissionNeeded = notEmpty(config, "permissionNeeded");
    // Preconditions.checkArgument(!permissionNeeded.isEmpty(), "%s permissionNeeded must not be
    // empty", location(config, "permissionNeeded"));
  }

  public RouteConfig(List<HttpMethod> methods, String host, String path, String permissionNeeded) {
    ensureNotEmpty("permissionNeeded", permissionNeeded);
    ensureNotEmpty("path", path);
    ensureStartsWith("path", path, "/");

    this.methods = methods;
    this.host = host;
    this.path = path;
    this.permissionNeeded = permissionNeeded;
  }
}
