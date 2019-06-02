package com.nikitavbv.photostorage;

import static com.kosprov.jargon2.api.Jargon2.jargon2Hasher;

import com.kosprov.jargon2.api.Jargon2;
import com.nikitavbv.photostorage.models.ApplicationUser;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class ApiVerticle extends AbstractVerticle {

  protected Handler<Message<JsonObject>> addJsonConsumer(Function<JsonObject, Future<JsonObject>> consumer,
                                                       List<String> requiredFields) {
    return resp -> {
      JsonObject body = resp.body();
      if (body == null) {
        resp.reply(new JsonObject().put("status", "error").put("error", "empty_body"));
        return;
      }

      List<String> missingFields = requiredFields.stream().filter(k -> !body.containsKey(k))
              .collect(Collectors.toList());
      if (!missingFields.isEmpty()) {
        JsonObject result = new JsonObject()
                .put("status", "error")
                .put("error", "missing_fields")
                .put("missing_fields", missingFields);
        resp.reply(result);
        return;
      }

      consumer.apply(body).setHandler(res -> resp.reply(res.result()));
    };
  }

  protected Future<ApplicationUser> getUserBySessionToken(String sessionToken) {
    Future<ApplicationUser> future = Future.future();
    getUserIDBySessionToken(sessionToken).setHandler(userIDReply ->
            getUserByUserID(userIDReply.result()).setHandler(userReply -> future.complete(userReply.result())));
    return future;
  }

  protected Future<ApplicationUser> getUserByUserID(Integer userID) {
    Future<ApplicationUser> future = Future.future();

    JsonObject query = new JsonObject().put("id", userID);
    JsonObject getOp = new JsonObject()
            .put("table", "users")
            .put("query", query)
            .put("select_fields", new JsonArray().add("username").add("id"))
            .put("limit", 1);
    vertx.eventBus().send(EventBusAddress.DATABASE_GET, getOp, res -> {
      JsonArray rows = ((JsonObject) res.result().body()).getJsonArray("rows");
      JsonObject user = rows.getJsonObject(0);
      future.complete(new ApplicationUser(user));
    });

    return future;
  }

  protected Future<Integer> getUserIDBySessionToken(String sessionToken) {
    Future<Integer> future = Future.future();

    vertx.eventBus().send(EventBusAddress.CRYPTO_HASH_SESSION_TOKEN, sessionToken, (AsyncResult<Message<String>> hashedToken) -> {
      JsonObject query = new JsonObject()
              .put("access_token", hashedToken.result().body())
              .put("valid_until", System.currentTimeMillis());
      JsonObject getOp = new JsonObject()
              .put("table", "sessions")
              .put("query", query)
              .put("query_conditions", new JsonObject().put("valid_until", "<="))
              .put("select_fields", new JsonArray().add("user_id"))
              .put("limit", 1);
      vertx.eventBus().send(EventBusAddress.DATABASE_GET, getOp, res -> {
        JsonArray rows = ((JsonObject) res.result().body()).getJsonArray("rows");
        if (rows.size() == 0) {
          future.fail("invalid_token");
          return;
        }
        JsonObject session = rows.getJsonObject(0);
        future.complete(session.getInteger("user_id"));
      });
    });

    return future;
  }
}
