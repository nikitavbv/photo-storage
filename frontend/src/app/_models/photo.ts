import {CryptoService, PhotoService} from "../_services";

export class Photo {
  id: string;
  key: CryptoKey;
  key_enc: string;
  data_enc: string;
  data: string;

  uploaded_at: number;

  width: number;
  height: number;

  // meta
  description_enc: string;
  description: string;
  location_enc: string;
  location: string;
  tags_enc: string;
  tags: string[];

  constructor(private service: PhotoService, private crypto: CryptoService) {}

  load(): Promise<Photo> {
    return new Promise((resolve, reject) => {
      this.service.download(this.id).subscribe(res => {
        this.uploaded_at = res.uploaded_at;
        this.description_enc = res.description_enc;
        this.location_enc = res.location_enc;
        this.tags_enc = res.tags_enc;
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
        if (this.location_enc != null) {
          this.crypto.aesDecrypt(this.location_enc, key).then(data => {
            this.location = decoder.decode(data);
          });
        }
        if (this.tags_enc != null) {
          this.crypto.aesDecrypt(this.tags_enc, key).then(data => {
            this.tags = decoder.decode(data).split(':');
          });
        }
      }, console.error);
    }).then(() => this);
  }

  saveMetaUpdates() {
    const encoder = new TextEncoder();
    Promise.all<string>([
      this.crypto.aesEncrypt(encoder.encode(this.description), this.key),
      this.crypto.aesEncrypt(encoder.encode(this.location), this.key),
      this.crypto.aesEncrypt(encoder.encode(this.tags.join(':')), this.key)
    ]).then(([description, location, tags]) => {
      this.description_enc = description;
      this.location_enc = location;
      this.tags_enc = tags;
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

  removeTag(tag: string) {
    const index = this.tags.indexOf(tag);
    if (index !== -1) {
      this.tags.splice(index, 1);
      this.saveMetaUpdates();
    }
  }

  addTag(tag: string) {
    this.tags.push(tag);
    this.saveMetaUpdates();
  }

  download() {
    const link = document.createElement("a");
    link.download = this.id;
    link.href = this.data;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }
}
