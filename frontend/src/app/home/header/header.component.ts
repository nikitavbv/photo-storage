import {Component, EventEmitter, Output, ViewChild} from "@angular/core";
import {AuthenticationService, CryptoService} from "../../_services";
import {AlbumService} from "../../_services/album.service";

@Component({
  selector: 'header',
  templateUrl: 'header.component.html',
  styleUrls: ['header.component.less']
})
export class HeaderComponent {

  readonly NOTIF_SHOW_TIME = 5000;

  notifText: string = '';
  notifCloseTimeout: number = -1;
  showingNotif: boolean = false;
  searchQuery: string = '';
  creatingAlbum: boolean = false;
  newAlbumName: string = '';

  @ViewChild('fileInput') fileInput;

  @Output() filesToUploadSelected: EventEmitter<any> = new EventEmitter<any>();
  @Output() startSlideshow: EventEmitter<void> = new EventEmitter<void>();
  @Output() searchQueryUpdate: EventEmitter<string> = new EventEmitter<string>();

  constructor(private auth: AuthenticationService,
              private crypto: CryptoService,
              private albumService: AlbumService) {}

  notif(text: string): void {
    this.notifText = text;
    this.showingNotif = true;
    if (this.notifCloseTimeout != -1) {
      clearTimeout(this.notifCloseTimeout);
    }
    this.notifCloseTimeout = setTimeout(() => {
      this.showingNotif = false;
    }, this.NOTIF_SHOW_TIME);
  }

  selectFilesForUpload() {
    this.fileInput.nativeElement.click();
  }

  onFilesSelected(e: Event) {
    this.filesToUploadSelected.emit((e.target as any).files);
  }

  createAlbum() {
    const name = this.newAlbumName;

    Promise.all<CryptoKey, CryptoKey>([
      this.crypto.randomAESKey(),
      this.auth.publicKey()
    ]).then(([albumEncKey, publicKey]) => {
      return Promise.all<string, string>([
        this.crypto.aesEncrypt(new TextEncoder().encode(name), albumEncKey),
        this.crypto.encryptAESKeyWithPublicRSA(albumEncKey, publicKey)
      ]);
    }).then(([nameEnc, keyEnc]) => {
      this.albumService.create(nameEnc, keyEnc).subscribe(() => {}, console.error);
    });

    this.newAlbumName = '';
  }
}

