package com.nikitavbv.photostorage.api;

import com.nikitavbv.photostorage.ApiVerticle;
import com.nikitavbv.photostorage.EventBusAddress;
import com.nikitavbv.photostorage.models.ApplicationUser;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

@SuppressWarnings("Duplicates")
public class AlbumApiVerticle extends ApiVerticle {

  public void start() {
    vertx.eventBus().consumer(EventBusAddress.API_ALBUM_CREATE, addJsonConsumer(this::createAlbum, Arrays.asList(
        "access_token", "name", "key"
    )));
    vertx.eventBus().consumer(EventBusAddress.API_GET_MY_ALBUMS, addJsonConsumer(this::getMyAlbums, Collections.singletonList(
            "access_token"
    )));
    vertx.eventBus().consumer(EventBusAddress.API_SET_PHOTO_ALBUM, addJsonConsumer(this::setPhotoAlbum, Arrays.asList(
            "access_token", "photo_id", "album_id"
    )));
    vertx.eventBus().consumer(EventBusAddress.API_UNSET_PHOTO_ALBUM, addJsonConsumer(this::unsetPhotoAlbum, Arrays.asList(
            "access_token", "photo_id", "album_id"
    )));
    vertx.eventBus().consumer(EventBusAddress.API_GET_ALBUM_PHOTOS, addJsonConsumer(this::getAlbumPhotos, Arrays.asList(
            "access_token", "album_id"
    )));
  }

  private Future<JsonObject> getAlbumPhotos(JsonObject req) {
    Future<JsonObject> future = Future.future();
    final String albumID = req.getString("album_id");

    getUserBySessionToken(req.getString("access_token")).setHandler(getUserReply -> {
      ApplicationUser user = getUserReply.result();

      JsonObject photosQuery = new JsonObject()
              .put("album_id", albumID);
      JsonObject photosSelectOp = new JsonObject()
              .put("table", "album_photos")
              .put("query", photosQuery)
              .put("select_fields", new JsonArray().add("photo_id"));
      vertx.eventBus().send(EventBusAddress.DATABASE_GET, photosSelectOp, photosInfoRes -> {
        JsonArray photoRows = ((JsonObject) photosInfoRes.result().body()).getJsonArray("rows");

        JsonArray resultArr = new JsonArray();
        for (int i = 0; i < photoRows.size(); i++) {
          JsonObject row = photoRows.getJsonObject(i);
          resultArr.add(new JsonObject()
            .put("id", row.getString("photo_id")));
        }

        future.complete(new JsonObject().put("status", "ok").put("photos", resultArr));
      });
    });

    return future;
  }

  private Future<JsonObject> unsetPhotoAlbum(JsonObject req) {
    Future<JsonObject> future = Future.future();
    final String albumID = req.getString("album_id");
    final String photoID = req.getString("photo_id");

    getUserBySessionToken(req.getString("access_token")).setHandler(getUserReply -> {
      ApplicationUser user = getUserReply.result();

      JsonObject albumQuery = new JsonObject()
              .put("user_id", user.getID())
              .put("id", albumID);
      JsonObject albumSelectOp = new JsonObject()
              .put("table", "albums")
              .put("query", albumQuery)
              .put("select_fields", new JsonArray().add("id"));
      vertx.eventBus().send(EventBusAddress.DATABASE_GET, albumSelectOp, albumInfoRes -> {
        JsonArray albumRows = ((JsonObject) albumInfoRes.result().body()).getJsonArray("rows");
        if (albumRows.size() == 0) {
          future.complete(new JsonObject().put("error", "access_denied"));
          return;
        }

        JsonObject delQuery = new JsonObject()
                .put("album_id", albumID)
                .put("photo_id", photoID);
        JsonObject deleteEntryOp = new JsonObject()
                .put("table", "album_photos")
                .put("query", delQuery);
        vertx.eventBus().send(EventBusAddress.DATABASE_DELETE, deleteEntryOp, res ->
                future.complete(new JsonObject().put("status", "ok")));
      });
    });

    return future;
  }

  private Future<JsonObject> setPhotoAlbum(JsonObject req) {
    Future<JsonObject> future = Future.future();
    final String albumID = req.getString("album_id");
    final String photoID = req.getString("photo_id");

    getUserBySessionToken(req.getString("access_token")).setHandler(getUserReply -> {
      ApplicationUser user = getUserReply.result();

      JsonObject albumQuery = new JsonObject()
              .put("user_id", user.getID())
              .put("id", albumID);
      JsonObject albumSelectOp = new JsonObject()
              .put("table", "albums")
              .put("query", albumQuery)
              .put("select_fields", new JsonArray().add("id"));
      vertx.eventBus().send(EventBusAddress.DATABASE_GET, albumSelectOp, albumInfoRes -> {
        JsonArray albumRows = ((JsonObject) albumInfoRes.result().body()).getJsonArray("rows");
        if (albumRows.size() == 0) {
          future.complete(new JsonObject().put("error", "access_denied"));
          return;
        }

        JsonObject setAlbum = new JsonObject()
                .put("album_id", albumID)
                .put("photo_id", photoID);
        JsonObject insertEntryOp = new JsonObject()
                .put("table", "album_photos")
                .put("data", setAlbum);
        vertx.eventBus().send(EventBusAddress.DATABASE_INSERT, insertEntryOp, res ->
                future.complete(new JsonObject().put("status", "ok")));
      });
    });

    return future;
  }

  private Future<JsonObject> getMyAlbums(JsonObject req) {
    Future<JsonObject> future = Future.future();

    getUserBySessionToken(req.getString("access_token")).setHandler(getUserReply -> {
      ApplicationUser user = getUserReply.result();

      JsonObject albumsQuery = new JsonObject()
              .put("user_id", user.getID());
      JsonObject albumsSelectOp = new JsonObject()
              .put("table", "albums")
              .put("query", albumsQuery)
              .put("select_fields", new JsonArray().add("name_enc").add("key").add("id"));
      vertx.eventBus().send(EventBusAddress.DATABASE_GET, albumsSelectOp, albumsInfoRes -> {
        JsonArray albumRows = ((JsonObject) albumsInfoRes.result().body()).getJsonArray("rows");

        JsonArray resultArr = new JsonArray();
        for (int i = 0; i < albumRows.size(); i++) {
          JsonObject row = albumRows.getJsonObject(i);
          resultArr.add(new JsonObject()
            .put("id", row.getString("id"))
            .put("key_enc", row.getString("key"))
            .put("name_enc", row.getString("name_enc")));
        }

        future.complete(new JsonObject().put("status", "ok").put("albums", resultArr));
      });
    });

    return future;
  }

  private Future<JsonObject> createAlbum(JsonObject req) {
    Future<JsonObject> future = Future.future();

    getUserBySessionToken(req.getString("access_token")).setHandler(getUserReply -> {
      ApplicationUser user = getUserReply.result();
      final String albumID = UUID.randomUUID().toString();
      JsonObject albumObject = new JsonObject()
              .put("id", albumID)
              .put("name_enc", req.getString("name"))
              .put("key", req.getString("key"))
              .put("user_id", user.getID());
      JsonObject insertOp = new JsonObject()
              .put("table", "albums")
              .put("data", albumObject);
      vertx.eventBus().send(EventBusAddress.DATABASE_INSERT, insertOp, res ->
              future.complete(new JsonObject().put("status", "ok").put("id", albumID)));
    });

    return future;
  }

}
