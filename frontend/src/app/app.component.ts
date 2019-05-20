import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map } from 'rxjs/operators';
import * as CryptoJS from 'crypto-js';  

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.less']
})
export class AppComponent implements OnInit {

  private photoSelected;
  private keyToBeUsed;
  private password = 'userPassword';
  private photos;
  private statusText = '';
  private totalPhotosInUpload = 0;

  constructor(
    private httpClient: HttpClient,
  ) {}

  ngOnInit() {
    this.reloadPhotoList();
  }

  reloadPhotoList() {
    this.httpClient.get('/api/v1/users/me/photos', {
      headers: {'Authorization': 'Bearer MUSke/HFzqsSJODXXDLY6w=='}
    }).pipe(map((res: any) => {
      return res;
    })).subscribe(res => {
      this.photos = [];
      res.photos.forEach(element => {
        this.addPhotoUrl(element.id);
      });
    }, console.error);
  }

  addPhotoUrl(photoId) {
    this.httpClient.get('/api/v1/photos/' + photoId, {
      headers: {'Authorization': 'Bearer MUSke/HFzqsSJODXXDLY6w=='}
    }).pipe(map((res: any) => {
      const image = CryptoJS.AES.decrypt(atob(res.photo_data_enc), res.key_enc).toString(CryptoJS.enc.Utf8);
      if (image.startsWith('data:')) {
        this.photos.push(image);
      }
      return res;
    })).subscribe(res => {
    
    }, console.error);
  }

  genRandomKey() {
    return Math.random().toString(36).substr(2, 8) + Math.random().toString(36).substr(2, 8);
  }

  fileSelected(file) {
    const fileReader = new FileReader();
    fileReader.onload = (e) => {
      this.keyToBeUsed = this.genRandomKey();
      const img = (e.target as any).result;
      const enc = CryptoJS.AES.encrypt(img, this.keyToBeUsed);
      this.photoSelected = enc.toString();
    };
    fileReader.readAsDataURL(file[0]);
    
  }

  fileUpload() {
    console.log('key');
    console.log(this.keyToBeUsed);
    this.httpClient.post('/api/v1/photos', {
      "access_token": "MUSke/HFzqsSJODXXDLY6w==",
      "photo_data_enc": btoa(this.photoSelected),
      "photo_data_iv": "_",
      "key_enc": this.keyToBeUsed
    }).pipe(map((res: any) => {
      return res;
    })).subscribe(console.log, console.error);
    console.log('upload');
  }

  onDragover(e) {
    e.preventDefault();
    e.stopPropagation();
    console.log(e);
    return false;
  }

  onDrag(e) {
    for (let i = 0; i < e.dataTransfer.files.length; i++) {
      const fileReader = new FileReader();
      this.totalPhotosInUpload++;
      this.updateStatus();
      fileReader.onload = (e) => {
        const vkey = this.genRandomKey();
        const img = (e.target as any).result;
        const enc = CryptoJS.AES.encrypt(img, vkey);
        this.httpClient.post('/api/v1/photos', {
          "access_token": "MUSke/HFzqsSJODXXDLY6w==",
          "photo_data_enc": btoa(enc.toString()),
          "photo_data_iv": "_",
          "key_enc": vkey
        }).pipe(map((res: any) => {
          this.totalPhotosInUpload--;
          if (this.totalPhotosInUpload == 0) {
            this.reloadPhotoList();
          }
          this.updateStatus();
          return res;
        })).subscribe(console.log, console.error);
      };
      fileReader.readAsDataURL(e.dataTransfer.files[i]);
    }
    e.preventDefault();
    e.stopPropagation();

    return false;
  }

  handleFiles(e) {
    console.log('handleFiles');
    console.log(e);
  }

  updateStatus() {
    if (this.totalPhotosInUpload == 0) {
      this.statusText = '';
    } else {
      this.statusText = 'Uploading ' + this.totalPhotosInUpload + ' photos...';
    }
  }

}
