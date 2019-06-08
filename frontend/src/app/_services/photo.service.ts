import {Injectable} from '@angular/core';
import {HttpClient} from "@angular/common/http";
import {Observable} from "rxjs";
import {UploadPhotoResponse} from "../_models/upload-photo-response";

@Injectable()
export class PhotoService {

  constructor(private httpClient: HttpClient) {
  }

  upload(photo_data_enc: string, key_enc: string): Observable<UploadPhotoResponse> {
    return this.httpClient.post<UploadPhotoResponse>('/api/v1/photos', { photo_data_enc, key_enc });
  }

}
