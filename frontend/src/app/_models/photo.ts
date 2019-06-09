import {CryptoService, PhotoService} from "../_services";

export class Photo {
  id: string;
  key_enc: string;
  data_enc: string;
  data: string;

  width: number;
  height: number;

  constructor(private service: PhotoService, private crypto: CryptoService) {}

  load(): Promise<Photo> {
    return new Promise((resolve, reject) => {
      this.service.download(this.id).subscribe(res => {
        this.key_enc = res.key_enc;
        this.data_enc = res.photo_data_enc;
        resolve(this);
      }, reject);
    });
  }

  loadAndDecrypt(privateKey: CryptoKey): Promise<Photo> {
    return this.load().then(() => {
      this.crypto.decryptAESKeyWithPrivateRSA(this.key_enc, privateKey).then(key => {
        this.crypto.aesDecrypt(this.data_enc, key).then(data => {
          this.data = new TextDecoder().decode(data);
          this.updateWidthAndHeight();
        });
      }, console.error);
    }).then(() => this);
  }

  updateWidthAndHeight() {
    const image = new Image();
    image.onload = () => {
      this.width = image.width;
      this.height = image.height;
    };
    image.src = this.data;
  }
}
