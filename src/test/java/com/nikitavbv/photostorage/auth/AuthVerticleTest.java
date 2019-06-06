package com.nikitavbv.photostorage.auth;

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
public class AuthVerticleTest {

  private Vertx vertx;

  @Before
  public void deployVerticle(TestContext context) {
    vertx = Vertx.vertx();
    vertx.deployVerticle(AuthVerticle.class.getName(), context.asyncAssertSuccess());
    vertx.deployVerticle(CryptoVerticle.class.getName(), context.asyncAssertSuccess());
  }

  @After
  public void undeployVerticle(TestContext context) {
    vertx.close(context.asyncAssertSuccess());
  }

  @Test
  public void testUserAddMissingFields(TestContext context) {
    final Async async = context.async();
    JsonObject req = new JsonObject();
    req.put("username", "testuser");
    vertx.eventBus().send(EventBusAddress.API_ADD_USER, req, res -> {
      JsonObject response = ((JsonObject) res.result().body());
      context.assertEquals("error", response.getString("status"));
      context.assertEquals("missing_fields", response.getString("error"));
      context.assertTrue(response.getJsonArray("missing_fields").contains("password"));
      async.complete();
    });
  }

  @Test
  public void testUserAdd(TestContext context) {
    final Async requestAsync = context.async();
    final Async dbAsync = context.async();

    JsonObject req = new JsonObject();
    req.put("username", "testuser");
    req.put("password", "testpassword");
    req.put("public_key", "01234567890123456");
    req.put("private_key_enc", "01234567890123456");
    req.put("private_key_salt", "123456");
    vertx.eventBus().consumer(EventBusAddress.DATABASE_INSERT, obj -> {
      JsonObject objToInsert = ((JsonObject) obj.body());
      JsonObject data = objToInsert.getJsonObject("data");
      context.assertEquals("users", objToInsert.getString("table"));
      context.assertEquals("testuser", data.getString("username"));
      context.assertEquals("01234567890123456", data.getString("public_key"));
      context.assertTrue(data.containsKey("password_salt"));
      context.assertTrue(data.containsKey("password_hash"));
      context.assertTrue(data.containsKey("private_key_salt"));
      obj.reply(new JsonObject().put("status", "ok"));
      dbAsync.complete();
    });
    vertx.eventBus().send(EventBusAddress.API_ADD_USER, req, res -> {
      context.assertEquals("ok", ((JsonObject) res.result().body()).getString("status"));
      requestAsync.complete();
    });
  }

  @Test
  public void testAuth(TestContext context) {
    final Async authReqAsync = context.async();
    final Async sessionInsertAsync = context.async();

    JsonObject req = new JsonObject()
            .put("username", "testuser")
            .put("password", "testpassword")
            .put("ip", "127.0.0.1")
            .put("user_agent", "test");

    vertx.eventBus().consumer(EventBusAddress.DATABASE_GET, getReq -> {
      JsonObject getReqJson = ((JsonObject) getReq.body());
      if (getReqJson.getString("table").equals("users")) {
        JsonObject userObj = new JsonObject();
        userObj.put("id", 42);
        userObj.put("username", "testuser");
        userObj.put("password_salt", "DNufqI3bpc53eV6GkCl3CQ==");
        userObj.put("password_hash", "3gK+76j+4MJ1KWBa6e9fcw==");
        getReq.reply(new JsonObject().put("rows", new JsonArray().add(userObj)));
      } else if (getReqJson.getString("table").equals("sessions")) {
        getReq.reply(new JsonObject().put("rows", new JsonArray().add(new JsonObject().put("user_id", 42))));
      }
    });

    vertx.eventBus().consumer(EventBusAddress.DATABASE_INSERT, insertReq -> {
      JsonObject insertReqJson = ((JsonObject) insertReq.body());
      JsonObject insertData = insertReqJson.getJsonObject("data");
      context.assertEquals("sessions", insertReqJson.getString("table"));
      context.assertEquals(42, insertData.getInteger("user_id"));
      context.assertTrue(insertData.containsKey("access_token"));
      context.assertTrue(insertData.containsKey("valid_until"));
      context.assertTrue(insertData.getLong("valid_until") > System.currentTimeMillis());
      context.assertEquals("127.0.0.1", insertData.getString("ip"));
      context.assertEquals("test", insertData.getString("user_agent"));
      insertReq.reply(new JsonObject().put("status", "ok"));
      sessionInsertAsync.complete();
    });

    vertx.eventBus().send(EventBusAddress.API_AUTH, req, res -> {
      JsonObject body = ((JsonObject) res.result().body());
      context.assertEquals("ok", body.getString("status"));
      context.assertTrue(body.containsKey("access_token"));

      String accessToken = body.getString("access_token");
      JsonObject getMeReq = new JsonObject().put("access_token", accessToken);
      vertx.eventBus().send(EventBusAddress.API_GET_ME, getMeReq, getMeRes -> {
        JsonObject meBody = ((JsonObject) getMeRes.result().body());
        context.assertEquals("ok", meBody.getString("status"));
        context.assertEquals("testuser", meBody.getJsonObject("user").getString("username"));

        authReqAsync.complete();
      });
    });
  }

}
