package com.nikitavbv.photostorage.auth;

import com.nikitavbv.photostorage.ApiVerticle;
import com.nikitavbv.photostorage.EventBusAddress;
import com.nikitavbv.photostorage.models.ApplicationUser;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;

public class AuthVerticle extends ApiVerticle {

  private static final int SALT_LENGTH = 16;
  private static final int SESSION_TOKEN_LENGTH = 16;
  private static final long SESSION_TOKEN_LIFETIME = 1000 * 60 * 60 * 24; // 1 day

  private SecureRandom secureRandom = new SecureRandom();

  @Override
  public void start() {
    final EventBus eventBus = vertx.eventBus();
    eventBus.consumer(EventBusAddress.API_ADD_USER, addJsonConsumer(this::createUser, Arrays.asList(
            "username", "password", "public_key", "private_key_enc", "private_key_salt"
    )));
    eventBus.consumer(EventBusAddress.API_AUTH, addJsonConsumer(this::authUser, Arrays.asList(
            "username", "password", "ip", "user_agent"
    )));
    eventBus.consumer(EventBusAddress.API_GET_ME, addJsonConsumer(this::getMe, Collections.singletonList(
            "access_token"
    )));
  }

  private Future<JsonObject> createUser(JsonObject req) {
    Future<JsonObject> result = Future.future();
    byte[] salt = generateRandomSalt();
    vertx.eventBus().send(EventBusAddress.CRYPTO_HASH_PASSWORD, new JsonObject()
            .put("password", req.getString("password"))
            .put("salt", salt), (AsyncResult<Message<String>> hashedPassword) -> {
      JsonObject userObject = new JsonObject();
      userObject.put("username", req.getValue("username"));
      userObject.put("password_salt", Base64.getEncoder().encodeToString(salt));
      userObject.put("password_hash", hashedPassword.result().body());
      userObject.put("public_key", req.getValue("public_key"));
      userObject.put("private_key_enc", req.getValue("private_key_enc"));
      userObject.put("private_key_salt", req.getValue("private_key_salt"));

      JsonObject insertOp = new JsonObject().put("table", "users").put("data", userObject);
      vertx.eventBus().send(EventBusAddress.DATABASE_INSERT, insertOp, res ->
              result.complete(new JsonObject().put("status", "ok")));
    });
    return result;
  }

  private Future<JsonObject> authUser(JsonObject req) {
    Future<JsonObject> result = Future.future();

    JsonObject query = new JsonObject().put("username", req.getString("username"));
    JsonObject getOp = new JsonObject()
            .put("table", "users")
            .put("query", query)
            .put("select_fields", new JsonArray()
                    .add("id")
                    .add("password_salt")
                    .add("password_hash")
                    .add("private_key_enc")
                    .add("private_key_salt")
            )
            .put("limit", 1);
    vertx.eventBus().send(EventBusAddress.DATABASE_GET, getOp, res -> {
      JsonArray rows = ((JsonObject) res.result().body()).getJsonArray("rows");
      if (rows.size() == 0) {
        result.complete(new JsonObject().put("status", "error").put("error", "user_not_found"));
        return;
      }

      ApplicationUser user = new ApplicationUser(rows.getJsonObject(0));

      byte[] salt = user.passphraseSaltBytes();
      byte[] hash = user.passphraseHashBytes();
      byte[] password = req.getString("password").getBytes();

      vertx.eventBus().send(EventBusAddress.CRYPTO_PASSWORD_HASH_VERIFY, new JsonObject()
              .put("password", password)
              .put("hash", hash)
              .put("salt", salt), (AsyncResult<Message<JsonObject>> verifyResult) -> {
        if (!verifyResult.result().body().getBoolean("result")) {
          result.complete(new JsonObject().put("status", "error").put("error", "password_mismatch"));
        } else {
          startSession(user.getID(), req.getString("ip"), req.getString("user_agent"))
                  .setHandler(startSessionResult -> result.complete(new JsonObject()
                          .put("status", "ok")
                          .put("access_token", startSessionResult.result())
                          .put("private_key_enc", user.privateKey())
                          .put("private_key_salt", user.privateKeySalt())
                  ));
        }
      });
    });

    return result;
  }

  private Future<JsonObject> getMe(JsonObject req) {
    Future<JsonObject> future = Future.future();

    getUserIDBySessionToken(req.getString("access_token")).setHandler(handler -> {
      if (!handler.succeeded()) {
        future.complete(new JsonObject().put("status", "error").put("error", handler.cause().getMessage()));
        return;
      }

      JsonObject query = new JsonObject().put("id", handler.result());
      JsonObject getOp = new JsonObject()
              .put("table", "users")
              .put("query", query)
              .put("select_fields", new JsonArray().add("username"))
              .put("limit", 1);
      vertx.eventBus().send(EventBusAddress.DATABASE_GET, getOp, res -> {
        JsonObject user = ((JsonObject) res.result().body()).getJsonArray("rows").getJsonObject(0);
        future.complete(new JsonObject().put("status", "ok").put("user", user));
      });
    });

    return future;
  }

  private Future<String> startSession(int userID, String ip, String userAgent) {
    final Future<String> future = Future.future();

    final byte[] token = generateRandomSessionToken();
    final String encodedToken = Base64.getEncoder().encodeToString(token);

    vertx.eventBus().send(EventBusAddress.CRYPTO_HASH_SESSION_TOKEN, encodedToken, (AsyncResult<Message<String>> hashedToken) -> {
      JsonObject session = new JsonObject()
              .put("user_id", userID)
              .put("access_token", hashedToken.result().body())
              .put("valid_until", System.currentTimeMillis() + SESSION_TOKEN_LIFETIME)
              .put("ip", ip)
              .put("user_agent", userAgent);
      JsonObject insertOp = new JsonObject()
              .put("table", "sessions")
              .put("data", session);
      vertx.eventBus().send(EventBusAddress.DATABASE_INSERT, insertOp, res -> future.complete(encodedToken));
    });

    return future;
  }

  private byte[] generateRandomSalt() {
    byte[] salt = new byte[SALT_LENGTH];
    secureRandom.nextBytes(salt);
    return salt;
  }

  private byte[] generateRandomSessionToken() {
    byte[] token = new byte[SESSION_TOKEN_LENGTH];
    secureRandom.nextBytes(token);
    return token;
  }

}
