import {Component, EventEmitter, Output} from "@angular/core";
import {AuthenticationService, CryptoService} from "../../../_services";

declare const cryptico: any;
declare const scrypt: any;
declare const buffer: any;

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
    let masterKeyEnc: string = undefined;
    let publicKey: string = undefined;
    let hashedPassword: string = '';

    Promise.all([
      this.crypto.randomRSAKey(),
      this.crypto.deriveAESKey(this.passphrase),
      this.auth.hashPassword(this.passphrase, passwordSalt)
    ]).then(([masterKey, key, hashedPassword]: any[]) => {
      Promise.all([
        this.crypto.encryptPrivateRSAKeyWithAES(masterKey.privateKey, key.key),
        this.crypto.exportRSAPublicKey(masterKey.publicKey)
      ]).then(([privateKeyEnc, publicKey]) => {
        this.makeAuthRequest(this.username, hashedPassword, passwordSalt, publicKey, masterKeyEnc, key.salt);
      });
    }).catch(console.error);
  }

  makeAuthRequest(username: string, password: string, publicKey: string, masterKey: string): void {
    this.auth.signUp(username, password, publicKey, masterKey)
      .subscribe(() => this.signedUp.emit());
  }
}
