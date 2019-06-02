package com.nikitavbv.photostorage.utils;

import static com.kosprov.jargon2.api.Jargon2.jargon2Hasher;
import static com.kosprov.jargon2.api.Jargon2.jargon2Verifier;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.kosprov.jargon2.api.Jargon2;
import com.nikitavbv.photostorage.utils.CryptoVerticle;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import org.junit.Test;

public class CryptoTest {

  @Test
  public void testPublicKeyEncryption() throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
    KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
    keyPairGenerator.initialize(2048);
    KeyPair keyPair = keyPairGenerator.generateKeyPair();

    byte[] message = "Secret message".getBytes();
    Cipher cipher = Cipher.getInstance("RSA");
    cipher.init(Cipher.ENCRYPT_MODE, keyPair.getPublic());
    byte[] encrypted = cipher.doFinal(message);

    Cipher decryptCipher = Cipher.getInstance("RSA");
    decryptCipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
    assertEquals("Secret message", new String(decryptCipher.doFinal(encrypted)));
  }

  @Test
  public void testArgon2Hashing() {
    SecureRandom random = new SecureRandom();
    byte[] salt = new byte[16];
    random.nextBytes(salt);

    Jargon2.Hasher hasher = jargon2Hasher()
            .type(Jargon2.Type.ARGON2d)
            .memoryCost(65536)
            .timeCost(3)
            .parallelism(4)
            .salt(salt)
            .hashLength(16)
            .password("password".getBytes());
    byte[] hash = hasher.rawHash();

    String saltBase64 = Base64.getEncoder().encodeToString(salt);
    String hashBase64 = Base64.getEncoder().encodeToString(hash);

    salt = Base64.getDecoder().decode(saltBase64);
    hash = Base64.getDecoder().decode(hashBase64);

    Jargon2.RawVerifier verifier = jargon2Verifier()
            .type(Jargon2.Type.ARGON2d)
            .memoryCost(65536)
            .timeCost(3)
            .parallelism(4)
            .salt(salt)
            .hash(hash)
            .password("password".getBytes());
    assertTrue(verifier.verifyRaw());
  }

  @Test
  public void testPasswordVerify() {
    CryptoVerticle verticle = new CryptoVerticle();
    Jargon2.RawVerifier verifier = jargon2Verifier()
            .type(Jargon2.Type.ARGON2d)
            .memoryCost(65536)
            .timeCost(3)
            .parallelism(4)
            .salt(Base64.getDecoder().decode("DNufqI3bpc53eV6GkCl3CQ=="))
            .hash(Base64.getDecoder().decode("3gK+76j+4MJ1KWBa6e9fcw=="))
            .password(verticle.sha512("testpassword".getBytes()));
    assertTrue(verifier.verifyRaw());
  }

}
