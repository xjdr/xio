package com.xjeffrose.xio.backend.server;

import java.beans.ConstructorProperties;
import lombok.Getter;

class Kraken {
  @Getter String title;
  @Getter String description;

  @ConstructorProperties({"title", "description"})
  Kraken(String title, String description) {
    this.title = title;
    this.description = description;
  }
}
