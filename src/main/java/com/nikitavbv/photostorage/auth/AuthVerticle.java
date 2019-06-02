package com.nikitavbv.photostorage.auth;

import static com.kosprov.jargon2.api.Jargon2.jargon2Hasher;
import static com.kosprov.jargon2.api.Jargon2.jargon2Verifier;

import com.kosprov.jargon2.api.Jargon2;
import com.nikitavbv.photostorage.ApiVerticle;
import com.nikitavbv.photostorage.EventBusAddress;
import com.nikitavbv.photostorage.models.ApplicationUser;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;

public class AuthVerticle extends ApiVerticle {

  private static final int SALT_LENGTH = 16;
  private static final int HASH_LENGTH = 16;
  private static final int HASH_MEMORY_COST = 65536; // 64MB
  private static final int SESSION_TOKEN_LENGTH = 16;
  private static final long SESSION_TOKEN_LIFETIME = 1000 * 60 * 60 * 24; // 1 day

  private SecureRandom secureRandom = new SecureRandom();

  @Override
  public void start() {
    final EventBus eventBus = vertx.eventBus();
    eventBus.consumer(EventBusAddress.API_ADD_USER, addJsonConsumer(this::createUser, Arrays.asList(
            "username", "password", "public_key", "private_key_enc"
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
    hashPassword(req.getString("password").getBytes(), salt, hash -> {
      JsonObject userObject = new JsonObject();
      userObject.put("username", req.getValue("username"));
      userObject.put("password_salt", Base64.getEncoder().encodeToString(salt));
      userObject.put("password_hash", Base64.getEncoder().encodeToString(hash));
      userObject.put("public_key", req.getValue("public_key"));
      userObject.put("private_key_enc", req.getValue("private_key_enc"));

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

      verifyPassword(password, hash, salt).setHandler(verifyPasswordResult -> {
        if (!verifyPasswordResult.result()) {
          result.complete(new JsonObject().put("status", "error").put("error", "password_mismatch"));
        } else {
          startSession(user.getID(), req.getString("ip"), req.getString("user_agent"))
                  .setHandler(startSessionResult -> result.complete(new JsonObject()
                          .put("status", "ok")
                          .put("access_token", startSessionResult.result())
                          .put("master_key_enc", user.masterKey())
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
    JsonObject session = new JsonObject()
            .put("user_id", userID)
            .put("access_token", encodedToken)
            .put("valid_until", System.currentTimeMillis() + SESSION_TOKEN_LIFETIME)
            .put("ip", ip)
            .put("user_agent", userAgent);
    JsonObject insertOp = new JsonObject()
            .put("table", "sessions")
            .put("data", session);
    vertx.eventBus().send(EventBusAddress.DATABASE_INSERT, insertOp, res -> future.complete(encodedToken));

    return future;
  }

  private void hashPassword(byte[] password, byte[] salt, Handler<byte[]> handler) {
    vertx.executeBlocking((Handler<Future<byte[]>>) future -> {
      byte[] hashedPassword = sha512(password);
      Jargon2.Hasher hasher = jargon2Hasher()
              .type(Jargon2.Type.ARGON2d)
              .memoryCost(HASH_MEMORY_COST)
              .timeCost(3)
              .parallelism(4)
              .salt(salt)
              .hashLength(HASH_LENGTH);

      future.complete(hasher.password(hashedPassword).rawHash());
    }, res -> handler.handle(res.result()));
  }

  private Future<Boolean> verifyPassword(byte[] password, byte[] hash, byte[] salt) {
    Future<Boolean> result = Future.future();

    vertx.executeBlocking((Handler<Future<Boolean>>) future -> {
            byte[] hashedPassword = sha512(password);
            future.complete(jargon2Verifier()
                    .type(Jargon2.Type.ARGON2d)
                    .memoryCost(HASH_MEMORY_COST)
                    .timeCost(3)
                    .parallelism(4)
                    .hash(hash)
                    .salt(salt)
                    .password(hashedPassword)
                    .verifyRaw());
    }, res -> result.complete(res.result()));

    return result;
  }

  byte[] sha512(byte[] input) {
    try {
      return MessageDigest.getInstance("SHA-512").digest(input);
    } catch(NoSuchAlgorithmException e) {
      throw new AssertionError(e);
    }
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
