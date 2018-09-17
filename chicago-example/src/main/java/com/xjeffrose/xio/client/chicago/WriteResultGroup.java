package com.xjeffrose.xio.client.chicago;

public class WriteResultGroup {
  private final int quorum;

  WriteResultGroup(int quorum) {
    this.quorum = quorum;
  }

  public boolean quorumAcheived(WriteResult result) {
    System.out.println("Quorum acheived!");
    return true;
  }
}
