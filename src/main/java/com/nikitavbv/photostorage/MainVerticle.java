package com.nikitavbv.photostorage;

import com.nikitavbv.photostorage.api.AlbumApiVerticle;
import com.nikitavbv.photostorage.api.PhotoApiVerticle;
import com.nikitavbv.photostorage.api.UserApiVerticle;
import com.nikitavbv.photostorage.auth.AuthVerticle;
import com.nikitavbv.photostorage.database.PostgreSQLVerticle;
import com.nikitavbv.photostorage.storage.FilesystemStorageVerticle;
import com.nikitavbv.photostorage.api.PhotoDownloadVerticle;
import com.nikitavbv.photostorage.api.PhotoUploadVerticle;
import com.nikitavbv.photostorage.utils.CryptoVerticle;
import com.nikitavbv.photostorage.storage.GoogleCloudStorageVerticle;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import java.util.Arrays;

public class MainVerticle extends AbstractVerticle {

  @Override
  public void start() {
    Arrays.asList(
            WebServerVerticle.class.getName(),
            AuthVerticle.class.getName(),
            PostgreSQLVerticle.class.getName(),
            PhotoUploadVerticle.class.getName(),
            FilesystemStorageVerticle.class.getName(),
            PhotoDownloadVerticle.class.getName(),
            PhotoApiVerticle.class.getName(),
            CryptoVerticle.class.getName(),
            GoogleCloudStorageVerticle.class.getName(),
            UserApiVerticle.class.getName(),
            AlbumApiVerticle.class.getName()
    ).forEach(vertx::deployVerticle);
  }

  public static void main(String[] args) {
    Vertx.vertx().deployVerticle(MainVerticle.class.getName());
  }

}