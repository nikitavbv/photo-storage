package com.nikitavbv.photostorage.utils;

import static com.kosprov.jargon2.api.Jargon2.jargon2Hasher;
import static com.kosprov.jargon2.api.Jargon2.jargon2Verifier;

import com.kosprov.jargon2.api.Jargon2;
import com.nikitavbv.photostorage.EventBusAddress;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class CryptoVerticle extends AbstractVerticle {

  private static final int HASH_LENGTH = 16;
  private static final int HASH_MEMORY_COST = 65536; // 64MB

  @Override
  public void start() {
    vertx.eventBus().consumer(EventBusAddress.CRYPTO_HASH_SESSION_TOKEN, (Message<String> msg) ->
            hash(Base64.getDecoder().decode(msg.body()), new byte[0], hashedToken ->
                    msg.reply(Base64.getEncoder().encodeToString(hashedToken))
    ));
    vertx.eventBus().consumer(EventBusAddress.CRYPTO_HASH_PASSWORD, (Message<JsonObject> msg) ->
            hash(msg.body().getString("password").getBytes(), msg.body().getBinary("salt"), hashedPassword ->
              msg.reply(Base64.getEncoder().encodeToString(hashedPassword))));
    vertx.eventBus().consumer(EventBusAddress.CRYPTO_PASSWORD_HASH_VERIFY, (Message<JsonObject> msg) -> {
      JsonObject body = msg.body();
      verifyHash(body.getBinary("password"), body.getBinary("hash"), body.getBinary("salt"))
              .setHandler(res -> msg.reply(new JsonObject().put("result", res.result())));
    });
  }

  private Future<Boolean> verifyHash(byte[] password, byte[] hash, byte[] salt) {
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

  private void hash(byte[] password, byte[] salt, Handler<byte[]> handler) {
    vertx.executeBlocking((Handler<Future<byte[]>>) future -> {
      byte[] hashedPassword = sha512(password);
      Jargon2.Hasher hasher = jargon2Hasher()
              .type(Jargon2.Type.ARGON2d)
              .memoryCost(HASH_MEMORY_COST)
              .timeCost(3)
              .parallelism(4)
              .hashLength(HASH_LENGTH);
      if (salt.length > 0) {
        hasher = hasher.salt(salt);
      } else {
        hasher = hasher.salt(password);
      }
      future.complete(hasher.password(hashedPassword).rawHash());
    }, res -> handler.handle(res.result()));
  }

  byte[] sha512(byte[] input) {
    try {
      return MessageDigest.getInstance("SHA-512").digest(input);
    } catch(NoSuchAlgorithmException e) {
      throw new AssertionError(e);
    }
  }
}
