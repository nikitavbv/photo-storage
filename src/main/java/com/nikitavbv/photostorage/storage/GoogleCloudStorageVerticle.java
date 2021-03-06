package com.nikitavbv.photostorage.storage;

import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.Acl;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.nikitavbv.photostorage.ApiVerticle;
import io.vertx.config.ConfigRetriever;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.stream.Collectors;

public class GoogleCloudStorageVerticle extends ApiVerticle {

  public static final String DRIVER_NAME = "gcs";
  private Storage storage;
  private Bucket bucket;

  @Override
  public void start() {
    ConfigRetriever.create(vertx).getConfig(ar -> {
      JsonObject config = ar.result();
      vertx.executeBlocking((Handler<Future<Boolean>>) future -> {
        try {
          this.storage = StorageOptions.newBuilder()
            .setCredentials(ServiceAccountCredentials.fromStream(new FileInputStream(config.getString("storage.gcs.credentials.file"))))
            .setProjectId(config.getString("storage.gcs.project_id"))
            .build()
            .getService();
          this.bucket = storage.get(config.getString("storage.gcs.bucket_name"));
          future.complete(true);
        } catch (IOException e) {
          e.printStackTrace();
        }
      }, (result) -> {});
    });
    vertx.eventBus().consumer("photo.driver." + DRIVER_NAME + ".api",
            addJsonConsumer(this::uploadPhoto, Arrays.asList("photo_data_enc", "photo_id")));
    vertx.eventBus().consumer("photo.driver." + DRIVER_NAME + ".download",
            addJsonConsumer(this::downloadPhoto, Collections.singletonList("key")));
  }

  private Future<JsonObject> uploadPhoto(JsonObject uploadPhotoReq) {
    Future<JsonObject> future = Future.future();
    String photoID = uploadPhotoReq.getString("photo_id");
    byte[] photoData = Base64.getDecoder().decode(uploadPhotoReq.getString("photo_data_enc"));

    vertx.executeBlocking((Handler<Future<Boolean>>) fut -> {
      BlobInfo blobInfo = storage.create(BlobInfo.newBuilder(bucket.getName(), photoID).build(), photoData);
      future.complete(new JsonObject().put("key", photoID).put("link", blobInfo.getMediaLink()));
      fut.complete(true);
    }, (result) -> {});


    return future;
  }

  private Future<JsonObject> downloadPhoto(JsonObject downloadPhotoReq) {
    Future<JsonObject> future = Future.future();
    String photoKey = downloadPhotoReq.getString("key");

    Blob photoBlob = bucket.get(photoKey);

    ByteBuffer res = ByteBuffer.allocate(photoBlob.getSize().intValue());
    try {
      photoBlob.reader().read(res);
      future.complete(new JsonObject().put("photo_data_enc", res.array()));
    } catch (IOException e) {
      e.printStackTrace();
    }

    return future;
  }

}
