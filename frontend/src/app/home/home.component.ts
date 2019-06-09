import {Component, HostListener, OnInit, ViewChild} from "@angular/core";
import {HeaderComponent} from "./header";
import {AuthenticationService, CryptoService, PhotoService} from "../_services";
import {Photo} from "../_models/photo";
import {GetMyPhotosResponse} from "../_models/get-my-photos-response";

@Component({
  selector: 'home',
  templateUrl: 'home.component.html',
  styleUrls: ['home.component.less']
})
export class HomeComponent implements OnInit {

  @ViewChild(HeaderComponent)
  private header: HeaderComponent;

  photoDragInProgress: boolean = false;
  totalFilesInUpload: number = 0;

  photos: Photo[];

  selectedPhoto: Photo;
  showingPhotoModal: boolean = false;

  constructor(private crypto: CryptoService, private photoService: PhotoService, private auth: AuthenticationService) {}

  ngOnInit() {
    Promise.all<GetMyPhotosResponse, CryptoKey>([
      this.photoService.get_my_photos(),
      this.auth.privateKey()
    ]).then(([photos, privateKey]) => {
      this.photos = photos.photos.map(photo => Object.assign(
        new Photo(this.photoService, this.crypto),
        photo
      ));
      this.photos.forEach(photo => photo.loadAndDecrypt(privateKey));
    }, console.error);
  }

  @HostListener('dragover', ['$event'])
  dragover(event: any): void {
    event.preventDefault();
    event.stopPropagation();
    this.photoDragInProgress = true;
  }

  @HostListener('drop', ['$event'])
  drop(event: any): void {
    event.preventDefault();
    event.stopPropagation();
    this.photoDragInProgress = false;
    this.uploadFiles(event.dataTransfer.files);
  }

  uploadFiles(files): void {
    for (let i = 0; i < files.length; i++) {
      this.startFileUpload(files[i]);
    }
  }

  @HostListener('dragend', ['$event'])
  @HostListener('dragexit', ['$event'])
  @HostListener('dragleave', ['$event'])
  dragend(event: any): void {
    event.preventDefault();
    event.stopPropagation();
    if (event.type === 'dragleave' && event.relatedTarget == null) {
      this.photoDragInProgress = false;
    }
  }

  startFileUpload(file: any): void {
    this.totalFilesInUpload++;
    this.updateUploadNotif();

    Promise.all<Uint8Array, CryptoKey, CryptoKey>([
      this.readFileAsDataUrl(file),
      this.crypto.randomAESKey(),
      this.auth.publicKey()
    ]).then(([data, photoEncKey, publicKey]) => {
      return Promise.all<string, string, string>([
        Promise.resolve<string>(new TextDecoder().decode(data)),
        this.crypto.aesEncrypt(data, photoEncKey),
        this.crypto.encryptAESKeyWithPublicRSA(photoEncKey, publicKey)
      ])
    }).then(([data, encryptedData, encryptedRsaKey]) => {
      this.photoService.upload(encryptedData, encryptedRsaKey).subscribe((res) => {
        console.log(res);
        this.totalFilesInUpload--;
        this.updateUploadNotif();
        const photo = new Photo(this.photoService, this.crypto);
        photo.data = data;
        this.photos.unshift(photo);
      }, console.error);
    });
  }

  updateUploadNotif() {
    if (this.totalFilesInUpload == 0) {
      this.header.notif(`All photos are uploaded`);
    } else if (this.totalFilesInUpload == 1) {
      this.header.notif(`Uploading ${this.totalFilesInUpload} photo...`);
    } else {
      this.header.notif(`Uploading ${this.totalFilesInUpload} photos...`);
    }
  }

  readFileAsDataUrl(file: Blob): Promise<Uint8Array> {
    return new Promise<Uint8Array>(resolve => {
      const fileReader = new FileReader();
      fileReader.onload = e => {
        resolve(new TextEncoder().encode((e.target as any).result.toString()));
      };
      fileReader.readAsDataURL(file);
    });
  }

  openPhotoModal(photo: Photo) {
    this.selectedPhoto = photo;
    this.showingPhotoModal = true;
  }
}

