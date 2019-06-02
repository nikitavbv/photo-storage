package com.nikitavbv.photostorage.api;

import com.nikitavbv.photostorage.ApiVerticle;
import com.nikitavbv.photostorage.EventBusAddress;
import com.nikitavbv.photostorage.models.ApplicationUser;
import com.nikitavbv.photostorage.storage.FilesystemStorageVerticle;
import com.nikitavbv.photostorage.storage.GoogleCloudStorageVerticle;
import io.vertx.config.ConfigRetriever;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import java.util.Arrays;
import java.util.UUID;

public class PhotoUploadVerticle extends ApiVerticle {

  private static final String DEFAULT_STORAGE_DRIVER = GoogleCloudStorageVerticle.DRIVER_NAME;

  private String storageDriver;

  public void start() {
    vertx.eventBus().consumer(EventBusAddress.API_PHOTO_UPLOAD, addJsonConsumer(this::uploadPhoto, Arrays.asList(
        "access_token", "photo_data_enc", "photo_data_iv", "key_enc"
    )));
    ConfigRetriever.create(vertx).getConfig(ar -> {
      JsonObject config = ar.result();
      storageDriver = config.getString("storage.driver", DEFAULT_STORAGE_DRIVER);
    });
  }

  private Future<JsonObject> uploadPhoto(JsonObject uploadPhotoRequest) {
    Future<JsonObject> future = Future.future();

    getUserBySessionToken(uploadPhotoRequest.getString("access_token")).setHandler(userReply -> {
      ApplicationUser user = userReply.result();
      String storageDriver = getPreferredStorageDriver(user);
      String photoID = UUID.randomUUID().toString();
      long uploadTimestamp = System.currentTimeMillis();
      String photoIV = uploadPhotoRequest.getString("photo_data_iv");
      String keyEnc = uploadPhotoRequest.getString("key_enc");

      JsonObject driverReq = new JsonObject()
              .put("photo_data_enc", uploadPhotoRequest.getString("photo_data_enc"))
              .put("photo_id", photoID);
      vertx.eventBus().send("photo.driver." + storageDriver + ".api", driverReq, driverResp -> {
          String storageKey = ((JsonObject) driverResp.result().body()).getString("key");

          JsonObject photoObject = new JsonObject()
                  .put("storage_driver", storageDriver)
                  .put("storage_key", storageKey)
                  .put("photo_data_iv", photoIV)
                  .put("id", photoID)
                  .put("upload_timestamp", uploadTimestamp);
          JsonObject insertOp = new JsonObject().put("table", "photos").put("data", photoObject);
          vertx.eventBus().send(EventBusAddress.DATABASE_INSERT, insertOp, res -> {
            JsonObject keyInfoObject = new JsonObject()
                    .put("photo_id", photoID)
                    .put("user_id", user.getID())
                    .put("key_enc", keyEnc);
            JsonObject keyInsertOp = new JsonObject().put("table", "keys").put("data", keyInfoObject);
            vertx.eventBus().send(EventBusAddress.DATABASE_INSERT, keyInsertOp, keySaveRes ->
                    future.complete(new JsonObject().put("status", "ok").put("id", photoID)));
          });
      });
    });

    return future;
  }

  @SuppressWarnings("unused")
  private String getPreferredStorageDriver(ApplicationUser user) {
      return storageDriver;
  }

}
