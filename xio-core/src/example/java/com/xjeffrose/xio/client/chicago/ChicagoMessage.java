package com.xjeffrose.xio.client.chicago;

import java.util.UUID;

public class ChicagoMessage {
  final UUID id;
  final Op op;
  final String colFam;
  final String key;
  final String val;
  protected ChicagoMessage(UUID id, Op op, String colFam, String key, String val) {
    this.id = id;
    this.op = op;
    this.colFam = colFam;
    this.key = key;
    this.val = val;
  }

  static ChicagoMessage write(UUID id, String colFam, String key, String val) {
    return new ChicagoMessage(id, Op.WRITE, colFam, key, val);
  }
}
