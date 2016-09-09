package com.xjeffrose.xio.client.mux;

import com.google.common.primitives.Ints;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode
public class Message {

  // RequestNoResponse
  // RequestExpectResponse
  // Response
  public static enum Op {
    Request,
    Response;

    public static Op fromBytes(byte[] opBytes) {
      return Op.values()[Ints.fromByteArray(opBytes)];
    }
  }

  @Getter
  final UUID id;

  @Getter
  final Op op;

  @Getter
  final String colFam;

  @Getter
  final String key;

  @Getter
  final String val;

  public RequestMuxerMessage() {
    id = UUID.randomUUID();
    op = Op.Request;
    colFam = "";
    key = "";
    val = "";
  }

  public RequestMuxerMessage(byte[] idBytes, byte[] opBytes, byte[] colFamBytes, byte[] keyBytes, byte[] valBytes) {
    id = UUID.fromString(new String(idBytes));
    op = Op.fromBytes(opBytes);
    colFam = new String(colFamBytes);
    key = new String(keyBytes);
    val = new String(valBytes);
  }

}
