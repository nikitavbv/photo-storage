package com.nikitavbv.photostorage.api;

import com.nikitavbv.photostorage.ApiVerticle;
import com.nikitavbv.photostorage.EventBusAddress;
import com.nikitavbv.photostorage.models.ApplicationUser;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.Arrays;
import java.util.Collections;

public class PhotoApiVerticle extends ApiVerticle {

  public void start() {
    vertx.eventBus().consumer(EventBusAddress.API_GET_MY_PHOTOS, addJsonConsumer(this::getUserPhotos,
            Collections.singletonList("access_token")));
    vertx.eventBus().consumer(EventBusAddress.API_PHOTO_UPDATE_META, addJsonConsumer(this::updatePhotoMeta,
            Arrays.asList("access_token", "photo_id", "description", "location", "tags")));
  }

  private Future<JsonObject> updatePhotoMeta(JsonObject req) {
    Future<JsonObject> future = Future.future();
    final String photoID = req.getString("photo_id");
    final String photoDescription = req.getString("description");
    final String photoLocation = req.getString("location");
    final String tags = req.getString("tags");

    getUserBySessionToken(req.getString("access_token")).setHandler(userReply -> {
      ApplicationUser user = userReply.result();

      JsonObject keysQuery = new JsonObject()
              .put("photo_id", photoID)
              .put("user_id", user.getID());
      JsonObject keysSelectOp = new JsonObject()
              .put("table", "keys")
              .put("query", keysQuery)
              .put("select_fields", new JsonArray().add("photo_id"));
      vertx.eventBus().send(EventBusAddress.DATABASE_GET, keysSelectOp, photoInfoRes -> {
        JsonArray keyRows = ((JsonObject) photoInfoRes.result().body()).getJsonArray("rows");
        if (keyRows.size() == 0) {
          future.complete(new JsonObject().put("error", "access_denied"));
          return;
        }

        JsonObject updateMeta = new JsonObject()
                .put("description", photoDescription)
                .put("location", photoLocation)
                .put("tags", tags);
        JsonObject updateMetaOp = new JsonObject()
                .put("table", "photos")
                .put("data", updateMeta)
                .put("query", new JsonObject().put("id", photoID))
                .put("limit", 1);
        vertx.eventBus().send(EventBusAddress.DATABASE_UPDATE, updateMetaOp, updateRes -> {
          future.complete(new JsonObject().put("status", "ok"));
        });
      });
    });

    return future;
  }

  private Future<JsonObject> getUserPhotos(JsonObject req) {
    Future<JsonObject> future = Future.future();

    getUserBySessionToken(req.getString("access_token")).setHandler(userReply -> {
      ApplicationUser user = userReply.result();

      JsonObject keysQuery = new JsonObject()
              .put("user_id", user.getID());
      JsonObject keysSelectOp = new JsonObject()
              .put("table", "keys")
              .put("query", keysQuery)
              .put("select_fields", new JsonArray().add("photo_id").add("key_enc"));
      vertx.eventBus().send(EventBusAddress.DATABASE_GET, keysSelectOp, photoInfoRes -> {
        JsonArray keysRows = ((JsonObject) photoInfoRes.result().body()).getJsonArray("rows");

        JsonArray resultArr = new JsonArray();
        for (int i = 0; i < keysRows.size(); i++) {
          JsonObject row = keysRows.getJsonObject(i);
          resultArr.add(
                  new JsonObject()
                  .put("id", row.getString("photo_id"))
                  .put("key_enc", row.getString("key_enc"))
          );
        }

        future.complete(new JsonObject().put("status", "ok").put("photos", resultArr));
      });
    });

    return future;
  }

}
