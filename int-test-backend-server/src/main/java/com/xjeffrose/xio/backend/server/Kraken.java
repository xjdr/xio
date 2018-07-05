package com.xjeffrose.xio.backend.server;

import lombok.Getter;

import java.beans.ConstructorProperties;

class Kraken {
  @Getter
  String title;
  @Getter String description;

  @ConstructorProperties({"title", "description"})
  Kraken(String title, String description) {
    this.title = title;
    this.description = description;
  }
}
