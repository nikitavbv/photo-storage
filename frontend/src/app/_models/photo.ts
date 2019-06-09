import {AuthenticationService, CryptoService, PhotoService} from "../_services";

export class Photo {
  id: string;
  key: CryptoKey;
  key_enc: string;
  data_enc: string;
  data: string;

  width: number;
  height: number;

  // meta
  description_enc: string;
  description: string;

  constructor(private service: PhotoService, private crypto: CryptoService) {}

  load(): Promise<Photo> {
    return new Promise((resolve, reject) => {
      this.service.download(this.id).subscribe(res => {
        this.description_enc = res.description_enc;
        this.key_enc = res.key_enc;
        this.data_enc = res.photo_data_enc;
        resolve(this);
      }, reject);
    });
  }

  loadAndDecrypt(privateKey: CryptoKey): Promise<Photo> {
    return this.load().then(() => {
      this.crypto.decryptAESKeyWithPrivateRSA(this.key_enc, privateKey).then(key => {
        this.key = key;
        const decoder = new TextDecoder();
        this.crypto.aesDecrypt(this.data_enc, key).then(data => {
          this.data = decoder.decode(data);
          this.updateWidthAndHeight();
        });
        if (this.description_enc != null) {
          this.crypto.aesDecrypt(this.description_enc, key).then(data => {
            this.description = decoder.decode(data);
          });
        }
      }, console.error);
    }).then(() => this);
  }

  saveMetaUpdates() {
    Promise.all<string>([
      this.crypto.aesEncrypt(new TextEncoder().encode(this.description), this.key)
    ]).then(([description]) => {
      this.description_enc = description;
      this.service.updateMeta(this).subscribe(() => {}, console.error);
    });
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
