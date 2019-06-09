package com.nikitavbv.photostorage.api;

import com.nikitavbv.photostorage.ApiVerticle;
import com.nikitavbv.photostorage.EventBusAddress;
import com.nikitavbv.photostorage.models.ApplicationUser;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.Arrays;

public class PhotoDownloadVerticle extends ApiVerticle {

  public void start() {
    vertx.eventBus().consumer(EventBusAddress.API_PHOTO_DOWNLOAD, addJsonConsumer(this::downloadPhoto, Arrays.asList(
        "access_token", "photo_id"
    )));
  }

  private Future<JsonObject> downloadPhoto(JsonObject downloadPhotoRequest) {
    Future<JsonObject> future = Future.future();

    getUserBySessionToken(downloadPhotoRequest.getString("access_token")).setHandler(userReply -> {
      ApplicationUser user = userReply.result();

      JsonObject keyQuery = new JsonObject()
              .put("photo_id", downloadPhotoRequest.getString("photo_id"))
              .put("user_id", user.getID());
      JsonObject keySelectOp = new JsonObject()
              .put("table", "keys")
              .put("query", keyQuery)
              .put("select_fields", new JsonArray().add("key_enc"))
              .put("limit", 1);
      vertx.eventBus().send(EventBusAddress.DATABASE_GET, keySelectOp, res -> {
        JsonArray rows = ((JsonObject) res.result().body()).getJsonArray("rows");
        if (rows.size() == 0) {
          future.complete(new JsonObject().put("status", "error").put("error", "not_found"));
          return;
        }

        String keyEnc = rows.getJsonObject(0).getString("key_enc");

        JsonObject photoInfoQuery = new JsonObject()
                .put("id", downloadPhotoRequest.getString("photo_id"));
        JsonObject photoSelectOp = new JsonObject()
                .put("table", "photos")
                .put("query", photoInfoQuery)
                .put("select_fields", new JsonArray()
                        .add("storage_driver")
                        .add("storage_key")
                        .add("description")
                        .add("location")
                        .add("tags")
                        .add("upload_timestamp")
                ).put("limit", 1);
        vertx.eventBus().send(EventBusAddress.DATABASE_GET, photoSelectOp, photoInfoRes -> {
          JsonArray photoInfoRows = ((JsonObject) photoInfoRes.result().body()).getJsonArray("rows");
          JsonObject photoInfo = photoInfoRows.getJsonObject(0);

          final String storageDriver = photoInfo.getString("storage_driver");
          final String storageKey = photoInfo.getString("storage_key");

          JsonObject driverReq = new JsonObject()
                  .put("key", storageKey);
          vertx.eventBus().send("photo.driver." + storageDriver + ".download", driverReq, driverResp -> {
            String photoData = ((JsonObject) driverResp.result().body()).getString("photo_data_enc");

            future.complete(new JsonObject()
                    .put("status", "ok")
                    .put("key_enc", keyEnc)
                    .put("photo_data_enc", photoData)
                    .put("uploaded_at", photoInfo.getLong("upload_timestamp"))
                    .put("description_enc", photoInfo.getString("description"))
                    .put("location_enc", photoInfo.getString("location"))
                    .put("tags_enc", photoInfo.getString("tags")));
          });
        });
      });
    });

    return future;
  }

}
