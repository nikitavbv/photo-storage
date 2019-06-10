package com.nikitavbv.photostorage.api;

import com.nikitavbv.photostorage.ApiVerticle;
import com.nikitavbv.photostorage.EventBusAddress;
import com.nikitavbv.photostorage.models.ApplicationUser;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import java.util.Collections;

public class UserApiVerticle extends ApiVerticle {

  public void start() {
    vertx.eventBus().consumer(EventBusAddress.API_GET_PUBLIC_KEY, addJsonConsumer(this::getUserPublicKey,
            Collections.singletonList("user")));
  }

  private Future<JsonObject> getUserPublicKey(JsonObject req) {
    Future<JsonObject> future = Future.future();
    final String username = req.getString("user");

    getUserByName(username).setHandler(userReply -> {
      ApplicationUser user = userReply.result();
      future.complete(new JsonObject().put("status", "ok")
              .put("id", user.getID())
              .put("public_key", user.publicKey()));
    });

    return future;
  }

}
