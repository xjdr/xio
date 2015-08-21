package com.xjeffrose.xio.core;

/**
 * This is just a concrete, usable wrapper for {@link HttpServerDefBuilderBase}, which can't be used
 * directly even if it was not abstract, because it uses a recursive generic definition.
 */
public class HttpServerDefBuilder extends HttpServerDefBuilderBase<HttpServerDefBuilder> {
}
