import {AlbumService} from "../_services/album.service";
import {AuthenticationService, CryptoService} from "../_services";

export class Album {
  id: string;
  name_enc: string;
  key_enc: string;

  key: CryptoKey;
  name: string;

  photos: string[];

  constructor(private service: AlbumService,
              private crypto: CryptoService,
              private auth: AuthenticationService) {
  }

  init() {
    this.auth.privateKey().then(privateKey => {
      this.crypto.decryptAESKeyWithPrivateRSA(this.key_enc, privateKey).then(aesKey => {
        this.key = aesKey;
        this.crypto.aesDecrypt(this.name_enc, this.key).then(decryptedName => {
          this.name = new TextDecoder().decode(decryptedName);
        });
      });
    });

    this.service.getPhotos(this.id).subscribe(res => {
      this.photos = res.photos.map(photo => photo.id);
      this.photos.forEach(photoID => this.service.photos[photoID] = this.id);
    }, console.error);
  }
}
