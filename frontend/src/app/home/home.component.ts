import {Component, HostListener, ViewChild} from "@angular/core";
import {HeaderComponent} from "./header";
import {AuthenticationService, CryptoService, PhotoService} from "../_services";

declare const cryptico: any;

@Component({
  selector: 'home',
  templateUrl: 'home.component.html',
  styleUrls: ['home.component.less']
})
export class HomeComponent {

  readonly NOTIF_SHOW_TIME = 5000;

  @ViewChild(HeaderComponent)
  private header: HeaderComponent;

  photoDragInProgress: boolean = false;
  totalFilesInUpload: number = 0;

  constructor(private crypto: CryptoService, private photoService: PhotoService, private auth: AuthenticationService) {}

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

    for (let i = 0; i < event.dataTransfer.files.length; i++) {
      this.startFileUpload(event.dataTransfer.files[i]);
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
    console.log('started');

    Promise.all<Uint8Array, CryptoKey, CryptoKey>([
      this.readFileAsDataUrl(file),
      this.crypto.randomAESKey(),
      this.auth.publicKey()
    ]).then(([data, photoEncKey, publicKey]) => {
      return Promise.all<string, string>([
        this.crypto.aesEncrypt(data, photoEncKey),
        this.crypto.encryptAESKeyWithPublicRSA(photoEncKey, publicKey)
      ])
    }).then(([encryptedData, encryptedRsaKey]) => {
      this.photoService.upload(encryptedData, encryptedRsaKey).subscribe((res) => {
        console.log(res);
        this.totalFilesInUpload--;
        this.updateUploadNotif();
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
}

