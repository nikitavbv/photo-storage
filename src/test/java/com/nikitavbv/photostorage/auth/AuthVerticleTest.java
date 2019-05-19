package com.nikitavbv.photostorage.auth;

import com.nikitavbv.photostorage.EventBusAddress;
import io.vertx.core.Vertx;
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
    vertx.eventBus().consumer(EventBusAddress.DATABASE_INSERT, obj -> {
      JsonObject objToInsert = ((JsonObject) obj.body());
      JsonObject data = objToInsert.getJsonObject("data");
      context.assertEquals("users", objToInsert.getString("table"));
      context.assertEquals("testuser", data.getString("username"));
      context.assertEquals("01234567890123456", data.getString("public_key"));
      context.assertTrue(data.containsKey("password_salt"));
      context.assertTrue(data.containsKey("password_hash"));
      obj.reply(new JsonObject().put("status", "ok"));
      dbAsync.complete();
    });
    vertx.eventBus().send(EventBusAddress.API_ADD_USER, req, res -> {
      context.assertEquals("ok", ((JsonObject) res.result().body()).getString("status"));
      requestAsync.complete();
    });
  }

}
