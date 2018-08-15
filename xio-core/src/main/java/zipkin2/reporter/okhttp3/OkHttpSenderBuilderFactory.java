package zipkin2.reporter.okhttp3;

import okhttp3.OkHttpClient;

public class OkHttpSenderBuilderFactory {
  public static OkHttpSender.Builder createSenderBuilder(OkHttpClient.Builder clientBuilder) {
    return new OkHttpSender.Builder(clientBuilder);
  }
}
