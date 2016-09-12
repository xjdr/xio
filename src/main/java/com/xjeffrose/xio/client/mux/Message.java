package com.xjeffrose.xio.client.mux;

import com.google.common.primitives.Ints;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Message(Request, Payload) - Request
 * Message(UUID) - Response
 */
@EqualsAndHashCode
public class Message {

  public static enum Op {
    RequestNoResponse,
    RequestExpectResponse,
    Response;

    public static Op fromBytes(byte[] opBytes) {
      return Op.values()[Ints.fromByteArray(opBytes)];
    }

    public byte toByte() {
      return (byte)ordinal();
    }

    public static Op fromByte(byte ordinal) {
      return Op.values()[ordinal];
    }
  }

  @Getter
  final Request request;

  @Getter
  final UUID id;

  @Getter
  final Op op;

  @Getter
  final Object payload;

  public Message(Request request, Object payload) {
    this.request = request;
    this.id = request.getId();
    if (request.expectsResponse()) {
      op = Op.RequestExpectResponse;
    } else {
      op = Op.RequestNoResponse;
    }
    this.payload = payload;
  }

  private Message(UUID id, Op op) {
    this.id = id;
    this.op = op;
    this.payload = null;
    request = null;
  }

  static public Message buildResponse(UUID id, Op op) {
    return new Message(id, op);
  }

  static public Message buildResponse(UUID id) {
    return new Message(id, Op.Response);
  }

}
