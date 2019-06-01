package com.nikitavbv.photostorage.models;

import io.vertx.core.json.JsonObject;
import java.util.Base64;

public class ApplicationUser {

  private final int id;
  private final String passphraseHash;
  private final String passphraseSalt;
  private final String masterKey;

  public ApplicationUser(JsonObject jsonObject) {
    this.id = jsonObject.getInteger("id");
    this.passphraseHash = jsonObject.getString("password_hash");
    this.passphraseSalt = jsonObject.getString("password_salt");
    this.masterKey = jsonObject.getString("private_key_enc");
  }

  public String masterKey() {
    return masterKey;
  }

  public byte[] passphraseHashBytes() {
    return Base64.getDecoder().decode(passphraseHash());
  }

  public byte[] passphraseSaltBytes() {
    return Base64.getDecoder().decode(passphraseSalt());
  }

  public String passphraseSalt() {
    return passphraseSalt;
  }

  public String passphraseHash() {
    return passphraseHash;
  }

  public Integer getID() {
    return id;
  }

}
