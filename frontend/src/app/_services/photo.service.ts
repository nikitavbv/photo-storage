import {Injectable} from '@angular/core';
import {HttpClient} from "@angular/common/http";
import {map} from "rxjs/operators";

@Injectable()
export class PhotoService {

  constructor(private httpClient: HttpClient) {
  }

  upload(photo_data_enc: string, key_enc: string) {
    this.httpClient.post('/api/v1/photos', { photo_data_enc, key_enc })
      .pipe(map((res: any) => {
        console.log('upload photo result:');
        return res;
      }));
  }

}
