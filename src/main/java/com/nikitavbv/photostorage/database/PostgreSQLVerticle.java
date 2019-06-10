package com.nikitavbv.photostorage.database;

import com.nikitavbv.photostorage.EventBusAddress;
import io.reactiverse.pgclient.PgClient;
import io.reactiverse.pgclient.PgIterator;
import io.reactiverse.pgclient.PgPool;
import io.reactiverse.pgclient.PgPoolOptions;
import io.reactiverse.pgclient.PgRowSet;
import io.reactiverse.pgclient.Row;
import io.reactiverse.pgclient.Tuple;
import io.vertx.config.ConfigRetriever;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@SuppressWarnings("Duplicates")
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
    eventBus.consumer(EventBusAddress.DATABASE_GET, this::databaseGet);
    eventBus.consumer(EventBusAddress.DATABASE_UPDATE, this::databaseUpdate);
    eventBus.consumer(EventBusAddress.DATABASE_DELETE, this::databaseDelete);
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

    Tuple values = Tuple.tuple();
    fields.forEach(key -> {
      Object value = data.getValue(key);
      if (value instanceof String) {
        values.addString((String) value);
      } else if (value instanceof Integer) {
        values.addInteger((Integer) value);
      } else if (value instanceof Long) {
        values.addLong((Long) value);
      } else {
        throw new AssertionError("Unexpected type: " + value.getClass().getName());
      }
    });

    String sql = "INSERT INTO " +
            tableName +
            " (" +
            String.join(",", fields) +
            ") VALUES (" +
            IntStream.range(1, fields.size() + 1).mapToObj(i -> "$" + i).collect(Collectors.joining(",")) +
            ")";
    client.preparedQuery(sql, values, ar -> {
      if (ar.succeeded()) {
        req.reply(new JsonObject().put("status", "ok"));
      } else {
        System.err.println(ar.cause().getMessage());
        req.reply(new JsonObject().put("status", "error"));
      }
    });
  }

  private void databaseUpdate(Message<JsonObject> req) {
    final JsonObject updateObject = req.body();
    final JsonObject data = updateObject.getJsonObject("data");
    final JsonObject query = updateObject.getJsonObject("query");
    final List<String> updateFields = new ArrayList<>(data.fieldNames());
    final List<String> selectFields = new ArrayList<>(query.fieldNames());
    final String tableName = updateObject.getString("table");

    StringBuilder sql = new StringBuilder();
    Tuple values = Tuple.tuple();
    sql.append("UPDATE ");
    sql.append(tableName);
    sql.append(" SET ");
    for (int i = 0; i < updateFields.size(); i++) {
      String field = updateFields.get(i);
      sql.append(field);
      sql.append(" = ");
      sql.append("$");
      sql.append(i + 1);

      if (i + 1< updateFields.size()) {
        sql.append(",");
      }

      sql.append(" ");

      Object fieldValue = data.getValue(field);
      if(fieldValue instanceof String) {
        values.addString(((String) fieldValue));
      } else {
        throw new AssertionError("Unexpected type: " + fieldValue.getClass().getName());
      }
    }

    sql.append("WHERE ");
    for (int i = 0; i < selectFields.size(); i++) {
      String field = selectFields.get(i);
      sql.append(field);
      sql.append(" = ");
      sql.append("$");
      sql.append(i + updateFields.size() + 1);
      if (i + 1 < selectFields.size()) {
        sql.append(" AND ");
      }

      Object fieldValue = query.getValue(field);
      if(fieldValue instanceof String) {
        values.addString(((String) fieldValue));
      } else {
        throw new AssertionError("Unexpected type: " + fieldValue.getClass().getName());
      }
    }

    client.preparedQuery(sql.toString(), values, ar -> {
      if (ar.succeeded()) {
        req.reply(new JsonObject().put("status", "ok"));
      } else {
        System.err.println(ar.cause().getMessage());
        req.reply(new JsonObject().put("status", "error"));
      }
    });
  }

  private void databaseGet(Message<JsonObject> req) {
    final JsonObject getObject = req.body();
    final JsonObject query = getObject.getJsonObject("query");
    final String tableName = getObject.getString("table");
    final List<String> fields = new ArrayList<>(query.fieldNames());
    final List<String> fieldsToSelect = new ArrayList<>();
    getObject.getJsonArray("select_fields").forEach(field -> fieldsToSelect.add(field.toString()));
    final int limit = getObject.getInteger("limit", -1);
    final JsonObject queryConditions = getObject.getJsonObject("query_conditions", new JsonObject());

    StringBuilder sql = new StringBuilder();
    Tuple values = Tuple.tuple();
    sql.append("SELECT ");
    sql.append(String.join(", ", fieldsToSelect));
    sql.append(" FROM ");
    sql.append(tableName);
    sql.append(" WHERE ");
    for (int i = 0; i < fields.size(); i++) {
      String field = fields.get(i);

      sql.append(field);
      sql.append(" ");
      sql.append(queryConditions.getString(field, "="));
      sql.append(" ");
      sql.append("$");
      sql.append(i + 1);

      if (i + 1 < fields.size()) {
        sql.append(" AND ");
      }

      Object fieldValue = query.getValue(field);
      if (fieldValue instanceof String) {
        values.addString((String) fieldValue);
      } else if (fieldValue instanceof Integer) {
        values.addInteger((Integer) fieldValue);
      } else if (fieldValue instanceof Long) {
        values.addLong((Long) fieldValue);
      } else {
        throw new AssertionError("Unexpected type: " + fieldValue.getClass().getName());
      }
    }

    if (limit != -1) {
      sql.append(" LIMIT ").append(limit);
    }

    client.preparedQuery(sql.toString(), values, ar -> {
      if (!ar.succeeded()) {
        req.reply(new JsonObject().put("status", "error"));
        return;
      }

      JsonArray result = new JsonArray();
      PgRowSet rowSet = ar.result();
      PgIterator iter = rowSet.iterator();

      int rows = 0;
      while (iter.hasNext()) {
        Row row = iter.next();
        JsonObject obj = new JsonObject();
        fieldsToSelect.forEach(key -> obj.put(key, row.getValue(key)));
        result.add(obj);
        rows++;
        if (rows == limit) {
          break;
        }
      }

      req.reply(new JsonObject().put("status", "ok").put("rows", result));
    });
  }

  private void databaseDelete(Message<JsonObject> req) {
    final JsonObject getObject = req.body();
    final JsonObject query = getObject.getJsonObject("query");
    final String tableName = getObject.getString("table");
    final List<String> fields = new ArrayList<>(query.fieldNames());

    StringBuilder sql = new StringBuilder();
    Tuple values = Tuple.tuple();
    sql.append("DELETE FROM ");
    sql.append(tableName);
    sql.append(" WHERE ");
    for (int i = 0; i < fields.size(); i++) {
      String field = fields.get(i);

      sql.append(field);
      sql.append(" = $");
      sql.append(i + 1);

      if (i + 1 < fields.size()) {
        sql.append(" AND ");
      }

      Object fieldValue = query.getValue(field);
      if (fieldValue instanceof String) {
        values.addString((String) fieldValue);
      } else if (fieldValue instanceof Integer) {
        values.addInteger((Integer) fieldValue);
      } else if (fieldValue instanceof Long) {
        values.addLong((Long) fieldValue);
      } else {
        throw new AssertionError("Unexpected type: " + fieldValue.getClass().getName());
      }
    }

    client.preparedQuery(sql.toString(), values, ar -> {
      if (!ar.succeeded()) {
        req.reply(new JsonObject().put("status", "error"));
        return;
      }

      req.reply(new JsonObject().put("status", "ok"));
    });
  }

}
