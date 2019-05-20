package com.nikitavbv.photostorage.api;

import com.nikitavbv.photostorage.EventBusAddress;
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
public class PhotoApiTest {

  private Vertx vertx;

  @Before
  public void deployVerticle(TestContext context) {
    vertx = Vertx.vertx();
    vertx.deployVerticle(PhotoApiVericle.class.getName(), context.asyncAssertSuccess());
  }

  @After
  public void undeployVerticle(TestContext context) {
    vertx.close(context.asyncAssertSuccess());
  }

  @Test
  public void getAllPhotos(TestContext context) {
    final Async async = context.async();

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
          sessionObj.put("access_token", "dummy_access_token");
          sessionObj.put("valid_until", System.currentTimeMillis() + 1000 * 60 * 10);
          getReq.reply(new JsonObject().put("rows", new JsonArray().add(sessionObj)));
          break;
        case "photos":
          JsonObject photoObj = new JsonObject();
          photoObj.put("storage_driver", "filesystem");
          photoObj.put("storage_key", "photo1");
          photoObj.put("photo_data_iv", "bb");
          JsonObject photoObj2 = new JsonObject();
          photoObj.put("storage_driver", "filesystem");
          photoObj.put("storage_key", "photo2");
          photoObj.put("photo_data_iv", "aa");
          getReq.reply(new JsonObject().put("rows", new JsonArray().add(photoObj).add(photoObj2)));
          break;
        case "keys":
          JsonObject keyObj = new JsonObject();
          keyObj.put("photo_id", "photo1");
          JsonObject keyObj2 = new JsonObject();
          keyObj2.put("photo_id", "photo2");
          getReq.reply(new JsonObject().put("rows", new JsonArray().add(keyObj).add(keyObj2)));
          break;
        default:
          System.err.println("Unmocked database req: " + getReqJson);
          break;
      }
    });

    JsonObject getMyPhotosReq = new JsonObject();
    getMyPhotosReq.put("access_token", "dummy_access_token");
    vertx.eventBus().send(EventBusAddress.API_GET_MY_PHOTOS, getMyPhotosReq, getMyPhotosResp -> {
      JsonObject myPhotosResult = ((JsonObject) getMyPhotosResp.result().body());
      context.assertEquals("ok", myPhotosResult.getString("status"));
      context.assertEquals("photo1", myPhotosResult.getJsonArray("photos").getJsonObject(0)
              .getString("id"));
      context.assertEquals("photo2", myPhotosResult.getJsonArray("photos").getJsonObject(1)
              .getString("id"));
      async.complete();
    });
  }

}
