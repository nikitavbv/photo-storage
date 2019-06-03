import {Injectable} from '@angular/core';

declare const cryptico: any;
declare const RSAKey: any;

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

  serializeRSAKey(key) {
    return JSON.stringify({
      coeff: key.coeff.toString(16),
      d: key.d.toString(16),
      dmp1: key.dmp1.toString(16),
      dmq1: key.dmq1.toString(16),
      e: key.e.toString(16),
      n: key.n.toString(16),
      p: key.p.toString(16),
      q: key.q.toString(16)
    });
  }

  deserializeRSAKey(key) {
    const json = JSON.parse(key);
    const rsa = new RSAKey();
    rsa.setPrivateEx(json.n, json.e, json.d, json.p, json.q, json.dmp1, json.dmq1, json.coeff);
    return rsa;
  }
}
