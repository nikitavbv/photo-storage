package com.nikitavbv.photostorage;

import io.vertx.config.ConfigRetriever;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

public class WebServerVerticle extends AbstractVerticle {

  private static final int DEFAULT_WEB_SERVER_PORT = 8080;

  @Override
  public void start(Future<Void> future) {
    ConfigRetriever.create(vertx).getConfig(ar -> {
      if (ar.failed()) {
        future.fail(new RuntimeException("Failed to load config"));
        return;
      }

      vertx.createHttpServer()
              .requestHandler(initRouter())
              .listen(ar.result().getInteger("http.server.port", DEFAULT_WEB_SERVER_PORT), result -> {
                if (result.succeeded()) {
                  future.complete();
                } else {
                  logError("Failed to start http server", result.cause());
                  future.fail(result.cause());
                }
              });
    });
  }

  private Router initRouter() {
    final Router router = Router.router(vertx);
    router.post("/api/v1/users").handler(BodyHandler.create()).handler(apiHandler(EventBusAddress.API_ADD_USER));
    router.post("/api/v1/auth").handler(BodyHandler.create()).handler(apiHandler(EventBusAddress.API_AUTH));
    router.get("/api/v1/users/me").handler(apiHandler(EventBusAddress.API_GET_ME));
    router.post("/api/v1/photos").handler(BodyHandler.create())
            .handler(apiHandler(EventBusAddress.API_PHOTO_UPLOAD));
    router.get("/api/v1/photos/:photo_id").handler(apiHandler(EventBusAddress.API_PHOTO_DOWNLOAD));
    return router;
  }

  private Handler<RoutingContext> apiHandler(String address) {
    return req -> {
      try {
        JsonObject body = req.getBodyAsJson();
        if (body == null) {
          body = new JsonObject();
        }
        body.put("ip", req.request().connection().remoteAddress().host());
        body.put("user_agent", req.request().getHeader("User-Agent"));
        if (req.request().headers().contains("Authorization")) {
          body.put("access_token", req.request().getHeader("Authorization").replace("Bearer ", ""));
        }

        for (String parameterKey : req.pathParams().keySet()) {
          body.put(parameterKey, req.pathParam(parameterKey));
        }

        vertx.eventBus().send(address, body, resp -> {
          if (resp.succeeded()) {
            req.response().end(resp.result().body().toString());
          } else {
            logError("Failed to get message reply", resp.cause());
            JsonObject respObject = new JsonObject().put("status", "error").put("error", "no_reply");
            req.response().setStatusCode(500).end(respObject.toString());
          }
        });
      } catch (Exception e) {
        e.printStackTrace();
      }
    };
  }

  private void logError(String message, Throwable error) {
    final String messageLine = String.format("%s: %s", message, error.getMessage());
    vertx.eventBus().send("log.error", new JsonObject().put("message", messageLine), resp -> {
      if (resp.failed()) {
        System.err.println(messageLine);
      }
    });
  }

}
