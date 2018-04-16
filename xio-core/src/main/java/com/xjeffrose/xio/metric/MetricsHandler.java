package com.xjeffrose.xio.metric;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.xjeffrose.xio.http.Request;
import com.xjeffrose.xio.http.Response;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

public class MetricsHandler extends ChannelDuplexHandler {
  private final MetricRegistry metricRegistry;
  private final Meter requestsMeter;
  private final Meter statusClassInformationalMeter;
  private final Meter statusClassSuccessMeter;
  private final Meter statusClassRedirectionMeter;
  private final Meter statusClassClientErrorMeter;
  private final Meter statusClassServerErrorMeter;
  private final Meter statusClassUnknownMeter;

  public MetricsHandler(MetricRegistry metricRegistry) {
    this.metricRegistry = metricRegistry;
    this.requestsMeter = metricRegistry.meter("requests");
    this.statusClassInformationalMeter = metricRegistry.meter("statusClassInformational");
    this.statusClassSuccessMeter = metricRegistry.meter("statusClassSuccess");
    this.statusClassRedirectionMeter = metricRegistry.meter("statusClassRedirection");
    this.statusClassClientErrorMeter = metricRegistry.meter("statusClassClientError");
    this.statusClassServerErrorMeter = metricRegistry.meter("statusClassServerError");
    this.statusClassUnknownMeter = metricRegistry.meter("statusClassUnknown");
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    super.channelRead(ctx, msg);

    if (msg instanceof Request) {
      Request request = (Request) msg;

      if (request.startOfMessage()) {
        requestsMeter.mark();
      }
    }
  }

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
    super.write(ctx, msg, promise);

    if (msg instanceof Response) {
      Response response = (Response) msg;

      if (response.startOfMessage()) {
        switch (response.status().codeClass()) {
          case INFORMATIONAL:
            statusClassInformationalMeter.mark();
            break;
          case SUCCESS:
            statusClassSuccessMeter.mark();
            break;
          case REDIRECTION:
            statusClassRedirectionMeter.mark();
            break;
          case CLIENT_ERROR:
            statusClassClientErrorMeter.mark();
            break;
          case SERVER_ERROR:
            statusClassServerErrorMeter.mark();
            break;
          case UNKNOWN:
            statusClassUnknownMeter.mark();
            break;
        }
      }
    }
  }
}
