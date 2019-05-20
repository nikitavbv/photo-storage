package com.nikitavbv.photostorage.storage;

import com.nikitavbv.photostorage.ApiVerticle;
import io.vertx.config.ConfigRetriever;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import java.io.File;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;

public class FilesystemStorageVerticle extends ApiVerticle {

  public static final String DRIVER_NAME = "filesystem";
  private static final String DEFAULT_PATH = "data/storage";

  private File storageFile;
  
  public void start() {
    ConfigRetriever.create(vertx).getConfig(ar -> {
      JsonObject config = ar.result();
      storageFile = new File(config.getString("storage.filesystem.path", DEFAULT_PATH));
      if (!storageFile.exists()) {
        if(!storageFile.mkdirs()) {
          throw new AssertionError("Failed to create storage directory");
        }
      }
    });

    vertx.eventBus().consumer("photo.driver." + DRIVER_NAME + ".api",
            addJsonConsumer(this::uploadPhoto, Arrays.asList("photo_data_enc", "photo_id")));
    vertx.eventBus().consumer("photo.driver." + DRIVER_NAME + ".download",
            addJsonConsumer(this::downloadPhoto, Collections.singletonList("key")));
  }

  private Future<JsonObject> uploadPhoto(JsonObject uploadPhotoReq) {
    Future<JsonObject> future = Future.future();
    String photoID = uploadPhotoReq.getString("photo_id");

    File photoDirectory = new File(storageFile, photoID.substring(0, 2));
    if (!photoDirectory.exists()) {
      if (!photoDirectory.mkdirs()) {
        throw new AssertionError("Failed to create storage sub-directory");
      }
    }

    File photoFile = new File(photoDirectory, photoID);
    Buffer photoData = Buffer.buffer();
    photoData.appendBytes(Base64.getDecoder().decode(uploadPhotoReq.getString("photo_data_enc")));
    vertx.fileSystem().writeFile(photoFile.getPath(), photoData, handler ->
            future.complete(new JsonObject().put("key", photoID)));

    return future;
  }

  private Future<JsonObject> downloadPhoto(JsonObject downloadPhotoReq) {
    Future<JsonObject> future = Future.future();
    String photoKey = downloadPhotoReq.getString("key");

    File photoDirectory = new File(storageFile, photoKey.substring(0, 2));
    File photoFile = new File(photoDirectory, photoKey);
    vertx.fileSystem().readFile(photoFile.getPath(), handler -> {
      String photoData = Base64.getEncoder().encodeToString(handler.result().getBytes());
      future.complete(new JsonObject().put("photo_data_enc", photoData));
    });

    return future;
  }

}
