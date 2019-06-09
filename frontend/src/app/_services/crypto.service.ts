import {Injectable} from '@angular/core';
import {CryptoKeyAndSalt} from "../_models/crypto-key-and-salt";

@Injectable()
export class CryptoService {

  readonly RSA_KEY_LENGTH = 2048;
  readonly RSA_PUBLIC_EXPONENT = new Uint8Array([0x01, 0x00, 0x01]);
  readonly RSA_HASH = 'SHA-512';
  readonly PBKDF2_SALT_LENGTH = 16;
  readonly PBKDF2_ITERATIONS = 1000;
  readonly PBKDF2_HASH = 'SHA-512';
  readonly AES_KEY_LENGTH = 256;
  readonly AES_IV_LENGTH = 12;
  readonly AES_TAG_LENGTH = 128;

  private worker: Worker;
  private callbacks = new Map();

  constructor() {
    this.worker = new Worker('/assets/crypto.worker.js');
    this.worker.onmessage = (msg) => {
      const id = msg.data.id;
      const callback = this.callbacks.get(id);
      callback(msg.data.key, msg.data.encrypted);
      this.callbacks.delete(id);
    };
  }

  aesEncrypt(dataToEncrypt: Uint8Array, key: CryptoKey): Promise<string> {
    return new Promise<string>(resolve => {
      const iv: Uint8Array = window.crypto.getRandomValues(new Uint8Array(this.AES_IV_LENGTH));
      window.crypto.subtle.encrypt(
        {
          name: 'AES-GCM',
          iv,
          tagLength: this.AES_TAG_LENGTH
        },
        key,
        dataToEncrypt
      ).then(encrypted => resolve(CryptoService.uInt8ArrayToString(
          CryptoService.ivAndDataToArray(iv, new Uint8Array(encrypted))
      )))
    });
  }

  aesDecrypt(dataToDecrypt: string, key: CryptoKey): Promise<Uint8Array> {
    return new Promise<Uint8Array>((resolve, reject) => {
      const bytes = CryptoService.stringToUInt8Array(dataToDecrypt);
      const iv = new Uint8Array(bytes[0]);
      const encryptedBytes = new Uint8Array(bytes.length - iv.length - 1);
      for (let i = 0; i < iv.length; i++) {
        iv[i] = bytes[i + 1];
      }
      for (let i = 0; i < encryptedBytes.length; i++) {
        encryptedBytes[i] = bytes[i + 1 + iv.length];
      }

      crypto.subtle.decrypt(
        {
          name: 'AES-GCM',
          iv,
          tagLength: this.AES_TAG_LENGTH
        },
        key,
        encryptedBytes
      ).then(data => resolve(new Uint8Array(data)), reject);
    });
  }

  rsaEncrypt(publicKey: CryptoKey, dataToEncrypt: Uint8Array): Promise<Uint8Array> {
    return new Promise<Uint8Array>(resolve => {
      window.crypto.subtle.encrypt(
        {
          name: 'RSA-OAEP'
        },
        publicKey,
        dataToEncrypt
      ).then(data => resolve(new Uint8Array(data)));
    });
  }

  rsaDecrypt(privateKey: CryptoKey, dataToDecrypt: Uint8Array): Promise<Uint8Array> {
    return new Promise<Uint8Array>(resolve => {
      window.crypto.subtle.decrypt(
        {
          name: 'RSA-OAEP'
        },
        privateKey,
        dataToDecrypt
      ).then(data => resolve(new Uint8Array(data)));
    });
  }

  randomRSAKey(): Promise<CryptoKeyPair> {
    return new Promise(resolve => {
      window.crypto.subtle.generateKey(
        {
          name: 'RSA-OAEP',
          modulusLength: this.RSA_KEY_LENGTH,
          publicExponent: this.RSA_PUBLIC_EXPONENT,
          hash: { name: this.RSA_HASH },
        },
        true,
        ['encrypt', 'decrypt']
      ).then(data => resolve(data));
    });
  }

  randomAESKey(): Promise<CryptoKey> {
    return new Promise((resolve, reject) => {
      window.crypto.subtle.generateKey(
        {
          name: 'AES-GCM',
          length: 256,
        },
        true,
        ['encrypt']
      ).then(resolve, reject);
    });
  }

  deriveAESKey(password: string, salt: string = undefined): Promise<CryptoKeyAndSalt> {
    const encodedPassword = new TextEncoder().encode(password);
    salt = salt || CryptoService.uInt8ArrayToString(
      window.crypto.getRandomValues(new Uint8Array(this.PBKDF2_SALT_LENGTH))
    );

    return new Promise(resolve => {
      window.crypto.subtle.importKey(
        'raw',
        encodedPassword,
        'PBKDF2',
        false,
        [ 'deriveKey', 'deriveBits' ]
      ).then(derivedKey => {
        window.crypto.subtle.deriveKey(
          {
            'name': 'PBKDF2',
            salt: CryptoService.stringToUInt8Array(salt),
            iterations: this.PBKDF2_ITERATIONS,
            hash: { name: this.PBKDF2_HASH },
          },
          derivedKey,
          {
            name: 'AES-GCM',
            length: this.AES_KEY_LENGTH
          },
          false,
          ['encrypt', 'decrypt']
        ).then((key) => resolve(new CryptoKeyAndSalt(key, salt)));
      });
    });
  }

  encryptPrivateRSAKeyWithAES(privateRSAKey: CryptoKey, aesKey: CryptoKey): Promise<string> {
    return new Promise((resolve, reject) => {
      window.crypto.subtle.exportKey(
        'pkcs8',
        privateRSAKey
      ).then(keyData => this.aesEncrypt(new Uint8Array(keyData), aesKey).then(resolve, reject));
    });
  }

  encryptAESKeyWithPublicRSA(aesKey: CryptoKey, publicRSAKey: CryptoKey): Promise<string> {
    return new Promise<string>((resolve, reject) => {
      window.crypto.subtle.exportKey('raw', aesKey).then(keyData => {
        this.rsaEncrypt(publicRSAKey, new Uint8Array(keyData)).then(data => {
          resolve(CryptoService.uInt8ArrayToString(data))
        }, reject);
      });
    });
  }

  decryptAESKeyWithPrivateRSA(aesKeyData: string, privateRSAKey: CryptoKey): Promise<CryptoKey> {
    return new Promise((resolve, reject) => {
      this.rsaDecrypt(privateRSAKey, CryptoService.stringToUInt8Array(aesKeyData)).then(keyData => {
        return window.crypto.subtle.importKey(
          'raw',
          keyData,
          'AES-GCM',
          false,
          ['decrypt']
        ).then(key => resolve(key), reject);
      });
    });
  }

  decryptPrivateRSAKeyWithAES(encrypted: string, aesKey: CryptoKey): Promise<CryptoKey> {
    const bytes = CryptoService.stringToUInt8Array(encrypted);
    const iv = new Uint8Array(bytes[0]);
    const encryptedBytes = new Uint8Array(bytes.length - iv.length - 1);
    for (let i = 0; i < iv.length; i++) {
      iv[i] = bytes[i + 1];
    }
    for (let i = 0; i < encryptedBytes.length; i++) {
      encryptedBytes[i] = bytes[i + 1 + iv.length];
    }

    return new Promise<CryptoKey>((resolve, reject) => {
      window.crypto.subtle.decrypt(
        {
          name: 'AES-GCM',
          iv,
          tagLength: this.AES_TAG_LENGTH
        },
        aesKey,
        encryptedBytes
      ).then(decryptedBytes => {
        window.crypto.subtle.importKey(
          'pkcs8',
          decryptedBytes,
          {
            name: 'RSA-OAEP',
            hash: {name: this.RSA_HASH},
          },
          true,
          ['decrypt']
        ).then(key => {
          resolve(key);
        }, reject);
      }, reject);
    });
  }

  exportRSAPublicKey(publicRSAKey: CryptoKey): Promise<string> {
    return new Promise(resolve => window.crypto.subtle.exportKey('spki', publicRSAKey)
      .then(keyData => resolve(CryptoService.arrayBufferToString(keyData)))
    );
  }

  importRSAPublicKey(publicKeyString: string): Promise<CryptoKey> {
    return new Promise((resolve, reject) => {
      crypto.subtle.importKey(
        'spki',
        CryptoService.stringToUInt8Array(publicKeyString),
        {
          name: 'RSA-OAEP',
          hash: { name: this.RSA_HASH }
        },
        false,
        ['encrypt']
      ).then(resolve, reject);
    });
  }

  exportRSAPrivateKey(privateRSAKey: CryptoKey): Promise<string> {
    return new Promise<string>(resolve => crypto.subtle.exportKey('pkcs8', privateRSAKey)
      .then(keyData => resolve(CryptoService.arrayBufferToString(keyData))));
  }

  importRSAPrivateKey(privateKeyString: string): Promise<CryptoKey> {
    return new Promise((resolve, reject) => {
      crypto.subtle.importKey(
        'pkcs8',
        CryptoService.stringToUInt8Array(privateKeyString),
        {
          name: 'RSA-OAEP',
          hash: { name: this.RSA_HASH }
        },
        false,
        ['decrypt']
      ).then(resolve, reject);
    });
  }

  static uInt8ArrayToString(arr: Uint8Array): string {
    let binary = '';
    const len = arr.byteLength;
    for (let i = 0; i < len; i++) {
      binary += String.fromCharCode(arr[i]);
    }
    return btoa(binary);
  }

  static stringToUInt8Array(str: string): Uint8Array {
    const binary = atob(str);
    const result = new Uint8Array(binary.length);
    for (let i = 0; i < result.length; i++) {
      result[i] = binary.charCodeAt(i);
    }
    return result;
  }

  static arrayBufferToString(buffer: ArrayBuffer): string {
    return CryptoService.uInt8ArrayToString(new Uint8Array(buffer));
  }

  static ivAndDataToArray(iv: Uint8Array, data: Uint8Array): Uint8Array {
    const result: Uint8Array = new Uint8Array(iv.length + data.length + 1);
    result[0] = iv.length;
    for (let i = 0; i < iv.length; i++) {
      result[i + 1] = iv[i];
    }
    for (let i = 0; i < data.length; i++) {
      result[i + iv.length + 1] = data[i];
    }
    return result;
  }
}
