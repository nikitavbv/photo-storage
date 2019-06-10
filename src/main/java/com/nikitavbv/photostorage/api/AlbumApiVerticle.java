package com.nikitavbv.photostorage.api;

import com.nikitavbv.photostorage.ApiVerticle;
import com.nikitavbv.photostorage.EventBusAddress;
import com.nikitavbv.photostorage.models.ApplicationUser;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import java.util.Arrays;
import java.util.UUID;

public class AlbumApiVerticle extends ApiVerticle {

  public void start() {
    vertx.eventBus().consumer(EventBusAddress.API_ALBUM_CREATE, addJsonConsumer(this::createAlbum, Arrays.asList(
        "access_token", "name", "key"
    )));
  }

  private Future<JsonObject> createAlbum(JsonObject req) {
    Future<JsonObject> future = Future.future();

    getUserBySessionToken(req.getString("access_token")).setHandler(getUserReply -> {
      ApplicationUser user = getUserReply.result();
      final String albumID = UUID.randomUUID().toString();
      JsonObject albumObject = new JsonObject()
              .put("id", albumID)
              .put("name_enc", req.getString("name"))
              .put("key", req.getString("key"))
              .put("user_id", user.getID());
      JsonObject insertOp = new JsonObject()
              .put("table", "albums")
              .put("data", albumObject);
      vertx.eventBus().send(EventBusAddress.DATABASE_INSERT, insertOp, res ->
              future.complete(new JsonObject().put("status", "ok").put("id", albumID)));
    });

    return future;
  }

}
