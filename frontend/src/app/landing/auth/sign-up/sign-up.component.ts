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
    let aesEnc: string = undefined;
    let publicKey: string = undefined;
    let hashedPassword: string = '';

    this.auth.hashPassword(this.passphrase, this.username, result => {
      hashedPassword = result;
      if (aesEnc) {
        this.makeAuthRequest(this.username, hashedPassword, publicKey, aesEnc);
      }
    });

    const masterKey =
    const key = cryptico.generateRSAKey(this.passphrase, this.auth.RSA_BITS);
    publicKey = cryptico.publicKeyString(key);
    aesEnc = cryptico.encrypt(aesKey, publicKey).cipher;

    if (hashedPassword) {
      this.makeAuthRequest(this.username, hashedPassword, publicKey, aesEnc);
    }
  }

  makeAuthRequest(username: string, password: string, publicKey: string, masterKey: string): void {
    this.auth.signUp(username, password, publicKey, masterKey)
      .subscribe(() => this.signedUp.emit());
  }
}
