package com.xjeffrose.xio.zookeeper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AwsIdentity {
  @JsonProperty("availabilityZone")
  String availabilityZone;

  @JsonProperty("instanceId")
  String instanceId;

  @JsonProperty("privateIp")
  String privateIp;

  public static AwsIdentity getIdentity(AwsDeploymentConfig config) throws IOException {
    OkHttpClient client = new OkHttpClient();
    Request request = new Request.Builder().url(config.getIdentityUrl()).build();

    try (Response response = client.newCall(request).execute()) {
      if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

      ObjectMapper objectMapper = new ObjectMapper();
      return objectMapper.readValue(response.body().string(), new TypeReference<AwsIdentity>() {});
    }
  }
}
