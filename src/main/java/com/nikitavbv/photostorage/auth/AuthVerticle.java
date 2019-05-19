package com.nikitavbv.photostorage.auth;

import static com.kosprov.jargon2.api.Jargon2.jargon2Hasher;

import com.kosprov.jargon2.api.Jargon2;
import com.nikitavbv.photostorage.EventBusAddress;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AuthVerticle extends AbstractVerticle {

  private static final int SALT_LENGTH = 16;
  private static final int HASH_LENGTH = 16;
  private static final int HASH_MEMORY_COST = 65536; // 64MB

  private SecureRandom secureRandom = new SecureRandom();

  @Override
  public void start() {
    final EventBus eventBus = vertx.eventBus();
    eventBus.consumer(EventBusAddress.API_ADD_USER, addJsonConsumer(this::createUser, Arrays.asList(
            "username", "password", "public_key"
    )));
  }

  private Handler<Message<JsonObject>> addJsonConsumer(Function<JsonObject, Future<JsonObject>> consumer,
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

  private Future<JsonObject> createUser(JsonObject req) {
    Future<JsonObject> result = Future.future();
    byte[] salt = generateRandomSalt();
    hashPassword(req.getString("password").getBytes(), salt, hash -> {
      JsonObject userObject = new JsonObject();
      userObject.put("username", req.getValue("username"));
      userObject.put("password_salt", Base64.getEncoder().encode(salt));
      userObject.put("password_hash", Base64.getEncoder().encode(hash));
      userObject.put("public_key", req.getValue("public_key"));

      JsonObject insertOp = new JsonObject().put("table", "users").put("data", userObject);
      vertx.eventBus().send(EventBusAddress.DATABASE_INSERT, insertOp, res ->
              result.complete(new JsonObject().put("status", "ok")));
    });
    return result;
  }

  private void hashPassword(byte[] password, byte[] salt, Handler<byte[]> handler) {
    vertx.executeBlocking((Handler<Future<byte[]>>) future -> {
      Jargon2.Hasher hasher = jargon2Hasher()
              .type(Jargon2.Type.ARGON2d)
              .memoryCost(HASH_MEMORY_COST)
              .timeCost(3)
              .parallelism(4)
              .salt(salt)
              .hashLength(HASH_LENGTH);

      future.complete(hasher.password(password).rawHash());
    }, res -> handler.handle(res.result()));
  }

  private byte[] generateRandomSalt() {
    byte[] salt = new byte[SALT_LENGTH];
    secureRandom.nextBytes(salt);
    return salt;
  }

}
