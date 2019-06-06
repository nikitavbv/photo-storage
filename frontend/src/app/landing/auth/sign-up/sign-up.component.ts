import {Component, EventEmitter, Output} from "@angular/core";
import {AuthenticationService, CryptoService} from "../../../_services";

@Component({
  selector: 'sign-up',
  templateUrl: 'sign-up.component.html',
  styleUrls: ['../auth.component.less']
})
export class SignUpComponent {

  @Output()
  signedUp = new EventEmitter();

  username: string = '';
  passphrase: string = '';
  passphraseRepeat: string = '';

  constructor(private auth: AuthenticationService, private crypto: CryptoService) {}

  signUp(): void {
    Promise.all([
      this.crypto.randomRSAKey(),
      this.crypto.deriveAESKey(this.passphrase),
    ]).then(([masterKey, key]: any[]) => {
      Promise.all([
        this.crypto.encryptPrivateRSAKeyWithAES(masterKey.privateKey, key.key),
        this.crypto.exportRSAPublicKey(masterKey.publicKey)
      ]).then(([privateKeyEnc, publicKey]: any[]) => {
        this.makeAuthRequest(this.username, this.passphrase, publicKey, privateKeyEnc, key.salt);
      });
    }).catch(console.error);
  }

  makeAuthRequest(username: string, password: string, publicKey: string, masterKey: string, masterKeySalt: string): void {
    this.auth.signUp(username, password, publicKey, masterKey, masterKeySalt)
      .subscribe(() => this.signedUp.emit());
  }
}
