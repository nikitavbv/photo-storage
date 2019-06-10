import {Injectable} from '@angular/core';
import {HttpClient} from "@angular/common/http";
import {Observable} from "rxjs";
import {FetchPublicKeyResponse} from "../_models/fetch-public-key-response";

@Injectable()
export class UserService {

  constructor(private httpClient: HttpClient) {
  }

  getPublicKey(username: string): Observable<FetchPublicKeyResponse> {
    return this.httpClient.get<FetchPublicKeyResponse>(`/api/v1/users/${username}/publicKey`);
  }
}
