import {Injectable} from '@angular/core';
import {HttpClient} from "@angular/common/http";
import {Observable} from "rxjs";
import {CreateAlbumResponse} from "../_models/create-album-response";

@Injectable()
export class AlbumService {

  constructor(private httpClient: HttpClient) {
  }

  create(name: string, key: string): Observable<CreateAlbumResponse> {
    return this.httpClient.post<CreateAlbumResponse>('/api/v1/albums', {
      name, key
    });
  }
}
