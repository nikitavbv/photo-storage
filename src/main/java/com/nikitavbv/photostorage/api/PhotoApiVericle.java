package com.nikitavbv.photostorage.api;

import com.nikitavbv.photostorage.ApiVerticle;
import com.nikitavbv.photostorage.EventBusAddress;
import com.nikitavbv.photostorage.models.ApplicationUser;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.Collections;

public class PhotoApiVericle extends ApiVerticle {

  public void start() {
    vertx.eventBus().consumer(EventBusAddress.API_GET_MY_PHOTOS, addJsonConsumer(this::getUserPhotos,
            Collections.singletonList("access_token")));
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
