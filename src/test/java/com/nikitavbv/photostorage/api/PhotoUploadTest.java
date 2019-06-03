package com.nikitavbv.photostorage.api;

import com.nikitavbv.photostorage.EventBusAddress;
import com.nikitavbv.photostorage.storage.FilesystemStorageVerticle;
import com.nikitavbv.photostorage.utils.CryptoVerticle;
import io.vertx.config.ConfigRetriever;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import java.security.AlgorithmParameters;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidParameterSpecException;
import java.util.Base64;
import java.util.Random;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class PhotoUploadTest {

  private Vertx vertx;

  @Before
  public void deployVerticle(TestContext context) {
    vertx = Vertx.vertx();
    vertx.deployVerticle(new PhotoUploadVerticle(FilesystemStorageVerticle.DRIVER_NAME), context.asyncAssertSuccess());
    vertx.deployVerticle(CryptoVerticle.class.getName(), context.asyncAssertSuccess());
  }

  @After
  public void undeployVerticle(TestContext context) {
    vertx.close(context.asyncAssertSuccess());
  }

  @Test
  public void testPhotoUpload(TestContext context) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, InvalidParameterSpecException, BadPaddingException, IllegalBlockSizeException {
    final Async uploadRequestAsync = context.async();

    byte[] photoData = new byte[10 * 1024];
    Random random = new Random();
    random.nextBytes(photoData);

    byte[] keyBytes = new byte[32];
    SecureRandom secureRandom = new SecureRandom();
    secureRandom.nextBytes(keyBytes);
    SecretKey key = new SecretKeySpec(keyBytes, "AES");

    Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
    cipher.init(Cipher.ENCRYPT_MODE, key);
    AlgorithmParameters params = cipher.getParameters();
    byte[] iv = params.getParameterSpec(IvParameterSpec.class).getIV();
    byte[] encryptedPhoto = cipher.doFinal(photoData);

    /**
     *Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
     *cipher.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(iv));
     *String plaintext = new String(cipher.doFinal(ciphertext), "UTF-8");
     *System.out.println(plaintext);
     */

    KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
    keyPairGenerator.initialize(2048);
    KeyPair keyPair = keyPairGenerator.generateKeyPair();
    Cipher rsaCipher = Cipher.getInstance("RSA");
    rsaCipher.init(Cipher.ENCRYPT_MODE, keyPair.getPublic());
    byte[] encryptedImageKey = cipher.doFinal(keyBytes);

    JsonObject photoUploadRequest = new JsonObject();
    photoUploadRequest.put("access_token", "fcrJKhGoGnjOyOKZ25up0A==");
    photoUploadRequest.put("photo_data_enc", Base64.getEncoder().encodeToString(encryptedPhoto));
    photoUploadRequest.put("photo_data_iv", Base64.getEncoder().encodeToString(iv));
    photoUploadRequest.put("key_enc", Base64.getEncoder().encodeToString(encryptedImageKey));

    vertx.eventBus().consumer(EventBusAddress.DATABASE_GET, getReq -> {
      JsonObject getReqJson = ((JsonObject) getReq.body());
      if (getReqJson.getString("table").equals("users")) {
        JsonObject userObj = new JsonObject();
        userObj.put("id", 42);
        userObj.put("username", "testuser");
        getReq.reply(new JsonObject().put("rows", new JsonArray().add(userObj)));
      } else if (getReqJson.getString("table").equals("sessions")) {
        JsonObject sessionObj = new JsonObject();
        sessionObj.put("user_id", 42);
        sessionObj.put("access_token", "fcrJKhGoGnjOyOKZ25up0A==");
        sessionObj.put("valid_until", System.currentTimeMillis() + 1000 * 60 * 10);
        getReq.reply(new JsonObject().put("rows", new JsonArray().add(sessionObj)));
      } else {
        System.err.println("Unmocked database req: " + getReqJson);
      }
    });

    vertx.eventBus().consumer("photo.driver.filesystem.api", getReq -> {
      JsonObject getReqJson = ((JsonObject) getReq.body());
      context.assertTrue(getReqJson.containsKey("photo_data_enc"));
      context.assertTrue(getReqJson.containsKey("photo_id"));
      getReq.reply(new JsonObject().put("key", getReqJson.getString("photo_id")));
    });

    vertx.eventBus().send(EventBusAddress.API_PHOTO_UPLOAD, photoUploadRequest, photoUploadResponse -> {
      JsonObject photoUploadResult = ((JsonObject) photoUploadResponse.result().body());
      context.assertEquals("ok", photoUploadResult.getString("status"));
      context.assertTrue(photoUploadResult.containsKey("id"));
      uploadRequestAsync.complete();
    });
  }

}
