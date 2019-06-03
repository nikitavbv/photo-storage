import {Injectable} from '@angular/core';
import {HttpClient} from "@angular/common/http";
import {Observable} from "rxjs";
import {AuthenticationResponse, GenericApiResponse} from "../_models";
import {map} from "rxjs/operators";

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

  constructor(private httpClient: HttpClient) {}

  signUp(username: string, password: string, publicKey: string, privateKeyEnc: string):
    Observable<GenericApiResponse> {
    return this.httpClient.post<GenericApiResponse>('/api/v1/users', {
      username,
      password,
      public_key: publicKey,
      private_key_enc: privateKeyEnc
    });
  }

  signIn(username: string, password: string): Observable<AuthenticationResponse> {
    const resObservable = Observable.create();

    this.hashPassword(password, username, hashedPassword => {
      this.httpClient.post<AuthenticationResponse>('/api/v1/auth', {
        username, password: hashedPassword
      }).pipe(map((res: AuthenticationResponse) => {
        const key = cryptico.generateRSAKey(password, this.RSA_BITS);
        console.log('master_key_enc:', res.master_key_enc);
        localStorage.setItem('access_token', res.access_token);
        localStorage.setItem('rsa_key', JSON.stringify(key));
        console.log(cryptico.string2bytes(cryptico.decrypt(res.master_key_enc, key).plaintext));
        localStorage.setItem('master_key', cryptico.decrypt(res.master_key_enc, key).plaintext);
        return res;
      })).subscribe(resObservable);
    });

    return resObservable;
  }

  hashPassword(password: string, salt: string, callback: (hashedPassword: string) => void) {
    const passwordBuffer = new buffer.SlowBuffer(password.normalize('NFKC'));
    const saltBuffer = new buffer.SlowBuffer(salt.normalize('NFKC'));
    scrypt(passwordBuffer, saltBuffer, this.SCRYPT_N, this.SCRYPT_R, this.SCRYPT_P, this.SCRYPT_DKLEN,
      (error, progress, key) => {
        if (error) {
          console.error('scrypt error:', error);
        } else if (key) {
          callback(btoa(cryptico.bytes2string(key)));
        }
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
    return localStorage.getItem('master_key');
  }
}
