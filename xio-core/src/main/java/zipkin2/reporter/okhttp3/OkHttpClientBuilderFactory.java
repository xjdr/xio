package zipkin2.reporter.okhttp3;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import okhttp3.OkHttpClient;
import org.apache.http.conn.ssl.NoopHostnameVerifier;

public class OkHttpClientBuilderFactory {
  public static OkHttpClient.Builder createZipkinClientBuilder() {
    TrustManager trustManager = new ZipkinTrustManager();
    TrustManager[] trustManagers = {trustManager};
    SSLContext sslCtx = null;
    try {
      sslCtx = SSLContext.getInstance("TLSv1.2");
      sslCtx.init(null, trustManagers, null);
    } catch (NoSuchAlgorithmException | KeyManagementException e) {
      System.exit(-1);
      throw new RuntimeException(e);
    }

    return new OkHttpClient.Builder()
        .sslSocketFactory(sslCtx.getSocketFactory(), (X509TrustManager) trustManager)
        .hostnameVerifier(new NoopHostnameVerifier());
  }

  private static class ZipkinTrustManager implements X509TrustManager {
    @Override
    public void checkClientTrusted(X509Certificate[] x509Certificates, String s)
        throws CertificateException {}

    @Override
    public void checkServerTrusted(X509Certificate[] x509Certificates, String s)
        throws CertificateException {}

    @Override
    public X509Certificate[] getAcceptedIssuers() {
      return new X509Certificate[0];
    }
  }
}
