package com.nikitavbv.photostorage;

import com.nikitavbv.photostorage.auth.AuthVerticle;
import com.nikitavbv.photostorage.database.PostgreSQLVerticle;
import com.nikitavbv.photostorage.storage.FilesystemStorageVerticle;
import com.nikitavbv.photostorage.upload.PhotoUploadVerticle;
import io.vertx.core.AbstractVerticle;
import java.util.Arrays;

public class MainVerticle extends AbstractVerticle {

  @Override
  public void start() {
    Arrays.asList(
            WebServerVerticle.class.getName(),
            AuthVerticle.class.getName(),
            PostgreSQLVerticle.class.getName(),
            PhotoUploadVerticle.class.getName(),
            FilesystemStorageVerticle.class.getName()
    ).forEach(vertx::deployVerticle);
  }

}