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

  isUsernameValid(): boolean {
    return this.username.length == 0 || this.username.length > 8;
  }

  isPassphraseValid(): boolean {
    return this.passphrase.length == 0 || this.passphrase.length > 8;
  }

  isPassphraseRepeatValid(): boolean {
    return this.passphraseRepeat.length == 0 || this.passphraseRepeat == this.passphrase;
  }

  isFormValid(): boolean {
    return this.isUsernameValid() && this.isPassphraseValid() && this.isPassphraseRepeatValid();
  }
}
