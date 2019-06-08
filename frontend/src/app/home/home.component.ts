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
    const fileReader = new FileReader();
    this.totalFilesInUpload++;
    if (this.totalFilesInUpload == 1) {
      this.header.notif(`Uploading ${this.totalFilesInUpload} photo...`);
    } else {
      this.header.notif(`Uploading ${this.totalFilesInUpload} photos...`);
    }
    console.log('started');
    fileReader.onload = (e) => {
      console.log('loaded');
      /*this.crypto.generateKeyAndEncrypt((e.target as any).result, (key, encryptedData) => {
        console.log('data encrypted');
        this.crypto.rsaEncrypt(cryptico.publicKeyString(this.auth.masterKey()), key, (encryptedKey) => {
          console.log('all done');
          console.log({ encryptedKey });
          console.log({ encryptedData });
        });
      });*/
    };
    fileReader.readAsDataURL(file);
  }
}

