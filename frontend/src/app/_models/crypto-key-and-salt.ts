export class CryptoKeyAndSalt {

  constructor(private key: CryptoKey, private salt: string) {}

  getKey() {
    return this.key;
  }

}
