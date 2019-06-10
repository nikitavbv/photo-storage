import {Injectable} from '@angular/core';
import {HttpClient} from "@angular/common/http";
import {Observable} from "rxjs";
import {CreateAlbumResponse} from "../_models/create-album-response";
import {GetMyAlbumsResponse} from "../_models/get-my-albums-response";
import {Album} from "../_models/album";
import {SetPhotoAlbumResponse} from "../_models/set-photo-album-reponse";
import {GetAlbumPhotosResponse} from "../_models/get-album-photos-response";
import {RemoveFromAlbumReponse} from "../_models/remove-from-album-reponse";

@Injectable()
export class AlbumService {

  photos: Object = {};
  albums: Album[];

  constructor(private httpClient: HttpClient) {
  }

  create(name: string, key: string): Observable<CreateAlbumResponse> {
    return this.httpClient.post<CreateAlbumResponse>('/api/v1/albums', {
      name, key
    });
  }

  get_my_albums(): Promise<GetMyAlbumsResponse> {
    return new Promise<GetMyAlbumsResponse>((reject, resolve) => {
      this.httpClient.get<GetMyAlbumsResponse>('/api/v1/users/me/albums').subscribe(reject, resolve);
    });
  }

  getPhotos(albumID: string): Observable<GetAlbumPhotosResponse> {
    return this.httpClient.get<GetAlbumPhotosResponse>(`/api/v1/albums/${albumID}/photos`);
  }

  setPhotoAlbum(photoID: string, album_id: string): Observable<SetPhotoAlbumResponse> {
    return this.httpClient.post<SetPhotoAlbumResponse>(`/api/v1/photos/${photoID}/album`, {
      album_id
    });
  }

  removeFromAlbum(photoID: string, album_id: string): Observable<RemoveFromAlbumReponse> {
    return this.httpClient.post<RemoveFromAlbumReponse>(`/api/v1/photos/${photoID}/album/unset`, {
      album_id
    });
  }

  inAlbum(id: string): boolean {
    return this.photos[id] !== undefined;
  }
}
