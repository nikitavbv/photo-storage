import {CryptoService} from "./crypto.service";
import {CryptoKeyAndSalt} from "../_models/crypto-key-and-salt";

describe('CryptoService', () => {
  let service: CryptoService;
  beforeEach(() => { service = new CryptoService(); });

  it('should encrypt and decrypt private rsa key', async (done: DoneFn) => {
    const passphrase = 'some passphrase';
    const dummyData: Uint8Array = new TextEncoder().encode('dummy data');
    const [rsaKey, derivedAESKey] = await Promise.all([
      service.randomRSAKey(),
      service.deriveAESKey(passphrase)
    ]);
    const dummyEncryptedData: Uint8Array = await service.rsaEncrypt(rsaKey.publicKey, dummyData);
    const privateKeyEnc: string =
      await service.encryptPrivateRSAKeyWithAES(rsaKey.privateKey, (derivedAESKey as any).key);
    const newKey: CryptoKeyAndSalt = await service.deriveAESKey(passphrase, (derivedAESKey as any).salt);
    const decryptedPrivateKey: CryptoKey = await service.decryptPrivateRSAKeyWithAES(privateKeyEnc, newKey.getKey());
    const dummyDecryptedData: Uint8Array = await service.rsaDecrypt(decryptedPrivateKey, dummyEncryptedData);
    const dummyDecryptedText: string = new TextDecoder().decode(dummyDecryptedData);
    expect(dummyDecryptedText).toBe('dummy data');
    done();
  });
});
