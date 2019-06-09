package com.nikitavbv.photostorage.api;

import com.nikitavbv.photostorage.EventBusAddress;
import com.nikitavbv.photostorage.utils.CryptoVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class PhotoDownloadTest {

  private Vertx vertx;

  @Before
  public void deployVerticle(TestContext context) {
    vertx = Vertx.vertx();
    vertx.deployVerticle(PhotoDownloadVerticle.class.getName(), context.asyncAssertSuccess());
    vertx.deployVerticle(CryptoVerticle.class.getName(), context.asyncAssertSuccess());
  }

  @After
  public void undeployVerticle(TestContext context) {
    vertx.close(context.asyncAssertSuccess());
  }

  @Test
  public void testDownload(TestContext context) {
    final Async downloadAsync = context.async();

    String photoID = "67dab16a-8cbf-4110-9af4-d45c37d48031";
    JsonObject photoDownloadRequest = new JsonObject();
    photoDownloadRequest.put("access_token", "fcrJKhGoGnjOyOKZ25up0A==");
    photoDownloadRequest.put("photo_id", photoID);

    vertx.eventBus().consumer(EventBusAddress.DATABASE_GET, getReq -> {
      JsonObject getReqJson = ((JsonObject) getReq.body());
      switch (getReqJson.getString("table")) {
        case "users":
          JsonObject userObj = new JsonObject();
          userObj.put("id", 42);
          userObj.put("username", "testuser");
          getReq.reply(new JsonObject().put("rows", new JsonArray().add(userObj)));
          break;
        case "sessions":
          JsonObject sessionObj = new JsonObject();
          sessionObj.put("user_id", 42);
          sessionObj.put("access_token", "87bc+2E1VhS6Uxl2Q1aRlA==");
          sessionObj.put("valid_until", System.currentTimeMillis() + 1000 * 60 * 10);
          getReq.reply(new JsonObject().put("rows", new JsonArray().add(sessionObj)));
          break;
        case "keys":
          JsonObject keysObj = new JsonObject();
          keysObj.put("user_id", 42);
          keysObj.put("photo_id", photoID);
          keysObj.put("key_enc", "aa");
          getReq.reply(new JsonObject().put("rows", new JsonArray().add(keysObj)));
          break;
        case "photos":
          JsonObject photoObj = new JsonObject();
          photoObj.put("storage_driver", "filesystem");
          photoObj.put("storage_key", photoID);
          getReq.reply(new JsonObject().put("rows", new JsonArray().add(photoObj)));
          break;
        default:
          System.err.println("Unmocked database req: " + getReqJson);
          break;
      }
    });

    vertx.eventBus().consumer("photo.driver.filesystem.download", getReq -> {
      JsonObject getReqJson = ((JsonObject) getReq.body());
      context.assertTrue(getReqJson.containsKey("key"));
      getReq.reply(new JsonObject().put("photo_data_enc", "00"));
    });

    vertx.eventBus().send(EventBusAddress.API_PHOTO_DOWNLOAD, photoDownloadRequest, photoDownloadResponse -> {
      JsonObject photoDownloadResult = ((JsonObject) photoDownloadResponse.result().body());
      context.assertEquals("ok", photoDownloadResult.getString("status"));
      context.assertTrue(photoDownloadResult.containsKey("photo_data_enc"));
      context.assertTrue(photoDownloadResult.containsKey("key_enc"));
      downloadAsync.complete();
    });
  }

}
