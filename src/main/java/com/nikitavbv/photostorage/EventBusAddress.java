package com.nikitavbv.photostorage;

public class EventBusAddress {

  public static final String API_ADD_USER = "auth.user.add";
  public static final String API_AUTH = "auth.user.auth";
  public static final String API_GET_ME = "users.get.me";
  public static final String API_PHOTO_UPLOAD = "photo.api";
  public static final String API_PHOTO_DOWNLOAD = "photo.download";
  public static final String API_GET_MY_PHOTOS = "users.get.me.photos";

  public static final String DATABASE_INSERT = "database.insert";
  public static final String DATABASE_GET = "database.get";

  public static final String CRYPTO_HASH_SESSION_TOKEN = "crypto.hash.token";
  public static final String CRYPTO_HASH_PASSWORD = "crypto.hash.password";
  public static final String CRYPTO_PASSWORD_HASH_VERIFY = "crypto.hash.password.verify";

}
