package com.xjeffrose.xio.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.util.AttributeKey;
import java.util.Map;
import java.util.HashMap;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Http2ProxyHandler extends SimpleChannelInboundHandler<Http2Request> {

  private static final AttributeKey<Map<Integer, Http2RouteProvider>> STREAM_ID_ROUTE_MAP_KEY = AttributeKey.newInstance("xio_stream_id_route_map_key");

  public static Map<Integer, Http2RouteProvider> getRouteMap(ChannelHandlerContext ctx) {
    Map<Integer, Http2RouteProvider> routeMap = ctx.channel().attr(STREAM_ID_ROUTE_MAP_KEY).get();
    if (routeMap == null) {
      routeMap = new HashMap<>();
      ctx.channel().attr(STREAM_ID_ROUTE_MAP_KEY).set(routeMap);
    }
    return routeMap;
  }

  public static void setRoute(ChannelHandlerContext ctx, int streamId, Http2RouteProvider route) {
    getRouteMap(ctx).put(streamId, route);
  }

  public static Http2RouteProvider getRoute(ChannelHandlerContext ctx, int streamId) {
    Map<Integer, Http2RouteProvider> routeMap = ctx.channel().attr(STREAM_ID_ROUTE_MAP_KEY).get();
    if (routeMap == null) {
      throw new RuntimeException("Couldn't find the route map for channel: " + ctx.channel());
    }
    Http2RouteProvider route = routeMap.get(streamId);

    if (route == null) {
      throw new RuntimeException("Coudln't find the route provider for stream id: " + streamId);
    }

    return route;
  }

  public static void closeRouteMap(ChannelHandlerContext ctx) {
    Map<Integer, Http2RouteProvider> routeMap = ctx.channel().attr(STREAM_ID_ROUTE_MAP_KEY).get();
    if (routeMap != null) {
      // TODO(CK): This is a little goofy we only want to call close once for each implementation
      //           of Http2RouterProvider. Not for every instance in the map.
      routeMap.values().stream().forEach((route) -> route.close(ctx));
    }
  }

  private final Http2UrlRouter router;

  public Http2ProxyHandler(Http2UrlRouter router) {
    this.router = router;
  }

  @Override
  public final void channelRead0(final ChannelHandlerContext ctx, Http2Request msg) throws Exception {
    if (msg.payload instanceof Http2Headers) {
      Http2Headers headers = (Http2Headers)msg.payload;
      Http2RouteProvider route = router.get(headers);
      setRoute(ctx, msg.streamId, route);

      route.handle(msg, ctx);
    } else if (msg.payload instanceof Http2DataFrame) {
      Http2DataFrame data = (Http2DataFrame)msg.payload;
      Http2RouteProvider route = getRoute(ctx, msg.streamId);

      route.handle(msg, ctx);
    }
  }

  @Override
  public final void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
    ctx.flush();
  }

  @Override
  public final void channelInactive(ChannelHandlerContext ctx) throws Exception {
    closeRouteMap(ctx);
    ctx.fireChannelInactive();
  }

  @Override
  public final void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    log.error("exceptionCaught", cause);
    ctx.close();
  }

}
