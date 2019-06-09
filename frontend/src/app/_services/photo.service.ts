import {Injectable} from '@angular/core';
import {HttpClient} from "@angular/common/http";
import {Observable} from "rxjs";
import {UploadPhotoResponse} from "../_models/upload-photo-response";
import {GetMyPhotosResponse} from "../_models/get-my-photos-response";
import {DownloadPhotoResponse} from "../_models/download-photo-response";

@Injectable()
export class PhotoService {

  constructor(private httpClient: HttpClient) {
  }

  get_my_photos(): Promise<GetMyPhotosResponse> {
    return new Promise((resolve, reject) => {
      this.httpClient.get<GetMyPhotosResponse>('/api/v1/users/me/photos').subscribe(resolve, reject);
    });
  }

  download(id: string): Observable<DownloadPhotoResponse> {
    return this.httpClient.get<DownloadPhotoResponse>(`/api/v1/photos/${id}`);
  }

  upload(photo_data_enc: string, key_enc: string): Observable<UploadPhotoResponse> {
    return this.httpClient.post<UploadPhotoResponse>('/api/v1/photos', { photo_data_enc, key_enc });
  }

}
