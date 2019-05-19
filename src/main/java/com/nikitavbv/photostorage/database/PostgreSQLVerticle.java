package com.nikitavbv.photostorage.database;

import com.nikitavbv.photostorage.EventBusAddress;
import io.reactiverse.pgclient.PgClient;
import io.reactiverse.pgclient.PgPool;
import io.reactiverse.pgclient.PgPoolOptions;
import io.reactiverse.pgclient.Tuple;
import io.vertx.config.ConfigRetriever;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PostgreSQLVerticle extends AbstractVerticle {

  private static final int DATABASE_DEFAULT_PORT = 5432;
  private static final String DATABASE_DEFAULT_HOST = "localhost";
  private static final String DATABASE_DEFAULT_USER = "devuser";
  private static final String DATABASE_DEFAULT_PASSWORD = "devpassword";
  private static final String DATABASE_DEFAULT_NAME = "photos";
  private static final int DATABASE_MAX_POOL_SIZE = 5;

  private PgPool client;

  @Override
  public void start(Future<Void> future) {
    setupDatabaseConnection().setHandler(res -> {
      if (res.succeeded()) {
        future.complete();
      } else {
        future.fail(res.cause());
      }
    });

    final EventBus eventBus = vertx.eventBus();
    eventBus.consumer(EventBusAddress.DATABASE_INSERT, this::databaseInsert);
  }

  @Override
  public void stop() {
    client.close();
  }

  private Future<Void> setupDatabaseConnection() {
    Future<Void> result = Future.future();
    ConfigRetriever.create(vertx).getConfig(ar -> {
      if (ar.failed()) {
        result.fail(new RuntimeException("Failed to load config"));
        return;
      }

      JsonObject config = ar.result();

      PgPoolOptions options = new PgPoolOptions()
              .setPort(config.getInteger("database.host", DATABASE_DEFAULT_PORT))
              .setHost(config.getString("database.port", DATABASE_DEFAULT_HOST))
              .setUser(config.getString("database.user", DATABASE_DEFAULT_USER))
              .setPassword(config.getString("database.password", DATABASE_DEFAULT_PASSWORD))
              .setDatabase(config.getString("database.name", DATABASE_DEFAULT_NAME))
              .setMaxSize(DATABASE_MAX_POOL_SIZE);

      client = PgClient.pool(vertx, options);
      result.complete();
    });
    return result;
  }

  private void databaseInsert(Message<JsonObject> req) {
    final JsonObject insertObject = req.body();
    final JsonObject data = insertObject.getJsonObject("data");
    final String tableName = insertObject.getString("table");
    final List<String> fields = new ArrayList<>(data.fieldNames());

    StringBuilder sql = new StringBuilder();
    sql.append("INSERT INTO ");
    sql.append(tableName);
    sql.append(" (");
    sql.append(String.join(",", fields));
    sql.append(") VALUES (");
    sql.append(IntStream.range(1, fields.size() + 1).mapToObj(i -> "$" + i).collect(Collectors.joining(",")));
    sql.append(")");

    Tuple values = Tuple.tuple();
    fields.forEach(key -> {
      Object value = data.getValue(key);
      if (value instanceof String) {
        values.addString((String) value);
      } else {
        throw new AssertionError("Unexpected type: " + value.getClass().getName());
      }
    });

    client.preparedQuery(sql.toString(), values, ar -> {
      if (ar.succeeded()) {
        req.reply(new JsonObject().put("status", "ok"));
      } else {
        System.err.println(ar.cause().getMessage());
        req.reply(new JsonObject().put("status", "error"));
      }
    });
  }

}
