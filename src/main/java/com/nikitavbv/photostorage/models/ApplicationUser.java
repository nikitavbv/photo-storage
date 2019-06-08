package com.nikitavbv.photostorage.models;

import io.vertx.core.json.JsonObject;
import java.util.Base64;

public class ApplicationUser {

  private final int id;
  private final String publicKey;
  private final String passphraseHash;
  private final String passphraseSalt;
  private final String privateKey;
  private final String privateKeySalt;

  public ApplicationUser(JsonObject jsonObject) {
    this.id = jsonObject.getInteger("id");
    this.passphraseHash = jsonObject.getString("password_hash");
    this.passphraseSalt = jsonObject.getString("password_salt");
    this.publicKey = jsonObject.getString("public_key");
    this.privateKey = jsonObject.getString("private_key_enc");
    this.privateKeySalt = jsonObject.getString("private_key_salt");
  }

  public String publicKey() {
    return publicKey;
  }

  public String privateKey() {
    return privateKey;
  }

  public String privateKeySalt() {
    return privateKeySalt;
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
