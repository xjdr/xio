package com.xjeffrose.xio.test;

import static java.net.HttpURLConnection.HTTP_VERSION;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Map;
import javax.net.ssl.HttpsURLConnection;
import okhttp3.Protocol;
import okhttp3.Response;

public class HttpURLConnectionUnsafe {

  public static Response unsafeRequest(String urlStr, String method, Map<String, String> headers)
      throws Exception {
    URL url = new URL(urlStr);
    HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
    con.setHostnameVerifier((s, sslSession) -> true);
    con.setSSLSocketFactory(
        OkHttpUnsafe.getUnsafeSSLSocketFactory(null, OkHttpUnsafe.unsafeTrustManager()));
    con.setRequestMethod(method);
    headers.forEach(con::setRequestProperty);
    con.setDoOutput(true);
    int responseCode = con.getResponseCode();
    BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
    String inputLine;
    StringBuilder response = new StringBuilder();
    while ((inputLine = in.readLine()) != null) {
      response.append(inputLine);
    }
    in.close();
    Response retVal =
        new Response.Builder()
            .message(response.toString())
            .protocol(Protocol.get(con.getHeaderFieldKey(HTTP_VERSION)))
            .code(responseCode)
            .build();
    con.disconnect();
    return retVal;
  }
}
