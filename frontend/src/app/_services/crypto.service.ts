import {Injectable} from '@angular/core';

declare const cryptico: any;

@Injectable()
export class CryptoService {

  readonly VALID_CHARS = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
  readonly RSA_KEY_LENGTH = 1024;

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
    let array = new Uint8Array(40);
    window.crypto.getRandomValues(array);
    array = array.map(x => this.VALID_CHARS.charCodeAt(x % this.VALID_CHARS.length));
    const randomState = String.fromCharCode.apply(null, array);
    return cryptico.generateRSAKey(randomState, this.RSA_KEY_LENGTH);
  }
}
