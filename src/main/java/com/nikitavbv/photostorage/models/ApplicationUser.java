package com.nikitavbv.photostorage.models;

import io.vertx.core.json.JsonObject;

public class ApplicationUser {

  private final int id;

  public ApplicationUser(JsonObject jsonObject) {
    this.id = jsonObject.getInteger("id");
  }

  public Integer getID() {
    return id;
  }

}
