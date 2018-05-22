package com.xjeffrose.xio.backend.server;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;

@ParametersAreNonnullByDefault
public class PojoResponse {
  private final String title;
  @Nullable
  private final String description;

  public PojoResponse(String title) {
    this(title, null);
  }

  public PojoResponse(String title, @Nullable String description) {
    this.title = title;
    this.description = description;
  }

  public String getTitle() {
    return title;
  }

  @Nonnull
  public Optional<String> getDescription() {
    return Optional.ofNullable(description);
  }
}
