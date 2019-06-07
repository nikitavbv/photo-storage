import {Injectable} from '@angular/core';

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

  generateKeyAndEncrypt(dataToEncrypt: any, callback: (key: number[], encryptedData: string) => void) {
    const id = CryptoService.randomId();
    this.callbacks.set(id, callback);
    this.worker.postMessage({
      dataToEncrypt,
      action: 'generate_key_and_encrypt',
      id
    });
  }

  rsaEncrypt(publicKey: any, dataToEncrypt: any, callback: (encryptedData: string) => void) {
    const id = CryptoService.randomId();
    this.callbacks.set(id, callback);
    this.worker.postMessage({
      publicKey,
      dataToEncrypt,
      action: 'encrypt_with_rsa_public_key',
      id
    });
  }

  aesEncrypt(key: any, dataToEncrypt: any, callback: (encryptedData: string) => void) {
    const id = CryptoService.randomId();
    this.callbacks.set(id, callback);
    this.worker.postMessage({
      key,
      dataToEncrypt,
      action: 'aes_encrypt',
      id
    });
  }

  static randomId(): string {
    return Math.random().toString(36);
  }

  randomRSAKey(): any {
    return window.crypto.subtle.generateKey(
      {
        name: 'RSA-OAEP',
        modulusLength: this.RSA_KEY_LENGTH,
        publicExponent: this.RSA_PUBLIC_EXPONENT,
        hash: { name: this.RSA_HASH },
      },
      true,
      ['encrypt', 'decrypt']
    );
  }

  deriveAESKey(password: string, salt: string = undefined) {
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
        ).then((key) => resolve({key, salt}));
      });
    });
  }

  encryptPrivateRSAKeyWithAES(privateRSAKey: CryptoKey, aesKey: CryptoKey) {
    return new Promise(resolve => {
      window.crypto.subtle.exportKey(
        'pkcs8',
        privateRSAKey
      ).then(keyData => {
        console.log('keyData is', keyData[0], keyData[1], keyData[3], keyData[4]);
        const iv: Uint8Array = window.crypto.getRandomValues(new Uint8Array(this.AES_IV_LENGTH));
        console.log('iv is', iv);
        window.crypto.subtle.encrypt(
          {
          name: 'AES-GCM',
          iv,
          tagLength: this.AES_TAG_LENGTH
          },
          aesKey,
          keyData
        ).then(encrypted => resolve(CryptoService.uInt8ArrayToString(iv) + ':' +
            CryptoService.arrayBufferToString(encrypted)))
      });
    });
  }

  decryptPrivateRSAKeyWithAES(encrypted: string, aesKey: CryptoKey) {
    const spl = encrypted.split(':');
    const iv = CryptoService.stringToUInt8Array(spl[0]);
    const encryptedBytes = CryptoService.stringToUInt8Array(spl[1]);

    console.log(encryptedBytes);

    return new Promise((resolve, reject) => {
      window.crypto.subtle.decrypt(
        {
          name: 'AES-GCM',
          iv,
          tagLength: this.AES_TAG_LENGTH
        },
        aesKey,
        encryptedBytes
      ).then(decryptedBytes => {
        console.log('decrypted private key:', decryptedBytes);

        window.crypto.subtle.importKey(
          'pkcs8',
          decryptedBytes,
          {
            name: 'RSA-OAEP',
            hash: {name: this.RSA_HASH},
          },
          false,
          ['decrypt']
        ).then(key => {
          resolve(key);
        }, console.error.bind(this, 'fail 2'));
      }, console.error.bind(this, 'fail 1'));
    });
  }

  exportRSAPublicKey(publicRSAKey: CryptoKey) {
    return new Promise(resolve => window.crypto.subtle.exportKey('spki', publicRSAKey)
      .then(keyData => resolve(CryptoService.arrayBufferToString(keyData)))
    );
  }

  static uInt8ArrayToArrayBuffer(arr: Uint8Array): ArrayBuffer {
    const buffer = new ArrayBuffer(arr.length);
    for (let i = 0; i < arr.length; i++) {
      buffer[i] = arr[i];
    }
    return buffer;
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
    const result = new Uint8Array(str.length);
    for (let i = 0; i < result.length; i++) {
      result[i] = binary.charCodeAt(i);
    }
    return result;
  }

  static arrayBufferToString(buffer: ArrayBuffer): string {
    return CryptoService.uInt8ArrayToString(new Uint8Array(buffer));
  }

  static stringToArrayBuffer(str: string): ArrayBuffer {
    return this.uInt8ArrayToArrayBuffer(this.stringToUInt8Array(str));
  }
}
