import {Injectable} from '@angular/core';
import {HttpClient} from "@angular/common/http";
import {Observable} from "rxjs";
import {AuthenticationResponse, GenericApiResponse} from "../_models";
import {map} from "rxjs/operators";
import {CryptoService} from "./crypto.service";

declare const cryptico: any;
declare const scrypt: any;
declare const buffer: any;

@Injectable()
export class AuthenticationService {

  readonly RSA_BITS = 1024;
  readonly SCRYPT_N = 1024;
  readonly SCRYPT_R = 8;
  readonly SCRYPT_P = 1;
  readonly SCRYPT_DKLEN = 32;

  constructor(private httpClient: HttpClient, private crypto: CryptoService) {}

  signUp(username: string, password: string, publicKey: string, privateKeyEnc: string, privateKeySalt):
    Observable<GenericApiResponse> {
    return this.httpClient.post<GenericApiResponse>('/api/v1/users', {
      username,
      password,
      public_key: publicKey,
      private_key_enc: privateKeyEnc,
      private_key_salt: privateKeySalt,
    });
  }

  signIn(username: string, password: string): Promise<AuthenticationResponse> {
    return new Promise((resolve, reject) => {
      this.httpClient.post<AuthenticationResponse>('/api/v1/auth', {
        username, password
      }).subscribe((res: AuthenticationResponse) => {
        console.log({res});
        this.crypto.deriveAESKey(password, res.private_key_salt).then((derivedKey: any) => {
          console.log({derivedKey});
          this.crypto.decryptPrivateRSAKeyWithAES(res.private_key_enc, derivedKey.key).then(decryptedPrivate => {
            console.log({ decryptedPrivate });
            resolve(res);
          }, reject);
        }, reject);
      }, reject);
    });
  }

  logout(): void {
    localStorage.removeItem('access_token');
    localStorage.removeItem('rsa_key');
    localStorage.removeItem('master_key');
  }

  // noinspection JSMethodCanBeStatic
  isLoggedIn(): boolean {
    return localStorage && localStorage.getItem('access_token') != undefined;
  }

  masterKey(): string {
    return 'fixme';
    // return this.crypto.deserializeRSAKey(localStorage.getItem('master_key'));
  }
}
