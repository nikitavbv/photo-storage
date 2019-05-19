package com.nikitavbv.photostorage;

import com.nikitavbv.photostorage.auth.AuthVerticle;
import com.nikitavbv.photostorage.database.PostgreSQLVerticle;
import io.vertx.core.AbstractVerticle;
import java.util.Arrays;

public class MainVerticle extends AbstractVerticle {

  @Override
  public void start() {
    Arrays.asList(
            WebServerVerticle.class.getName(),
            AuthVerticle.class.getName(),
            PostgreSQLVerticle.class.getName()
    ).forEach(vertx::deployVerticle);
  }

}