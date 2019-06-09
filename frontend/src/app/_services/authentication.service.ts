import {Injectable} from '@angular/core';
import {HttpClient} from "@angular/common/http";
import {Observable} from "rxjs";
import {AuthenticationResponse, GenericApiResponse} from "../_models";
import {CryptoService} from "./crypto.service";

@Injectable()
export class AuthenticationService {

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
        this.crypto.deriveAESKey(password, res.private_key_salt).then((derivedKey: any) => {
          this.crypto.decryptPrivateRSAKeyWithAES(res.private_key_enc, derivedKey.key).then(decryptedPrivate => {
            this.crypto.exportRSAPrivateKey(decryptedPrivate).then(exportedKey => {
              localStorage.setItem('access_token', res.access_token);
              localStorage.setItem('public_key', res.public_key);
              localStorage.setItem('private_key', exportedKey);
              resolve(res);
            }, reject);
          }, reject);
        }, reject);
      }, reject);
    });
  }

  logout(): void {
    localStorage.removeItem('access_token');
    localStorage.removeItem('public_key');
    localStorage.removeItem('private_key');
  }

  publicKey(): Promise<CryptoKey> {
    return this.crypto.importRSAPublicKey(localStorage.getItem('public_key'));
  }

  privateKey(): Promise<CryptoKey> {
    return this.crypto.importRSAPrivateKey(localStorage.getItem('private_key'));
  }

  // noinspection JSMethodCanBeStatic
  isLoggedIn(): boolean {
    return localStorage && localStorage.getItem('access_token') != undefined;
  }
}
