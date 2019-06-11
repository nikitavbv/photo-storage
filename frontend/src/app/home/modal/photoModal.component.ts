import {Component, EventEmitter, HostListener, Input, Output} from "@angular/core";
import {Photo} from "../../_models/photo";
import {AlbumService} from "../../_services/album.service";

@Component({
  selector: 'photo-modal',
  templateUrl: 'photoModal.component.html',
  styleUrls: ['photoModal.component.less']
})
export class PhotoModalComponent {

  readonly MONTHS = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];

  @Input() photo: Photo;
  @Output() closeModal: EventEmitter<void> = new EventEmitter();

  photoChanged: boolean = false;
  newTagName: string = '';
  shareWithUserName: string = '';

  prevAlbum: string = '';

  constructor(public albumService: AlbumService) {}

  @HostListener('document:keydown.escape', ['$event'])
  onKeydownHandler() {
    if (this.photoChanged) {
      this.photo.saveMetaUpdates();
    }
    this.closeModal.emit();
  }

  unixTimestampToStr(timestamp: number){
    const a = new Date(timestamp);
    const year = a.getFullYear();
    const month = this.MONTHS[a.getMonth()];
    const date = a.getDate();
    const hour = a.getHours();
    const min = a.getMinutes() < 10 ? ('0' + a.getMinutes()) : a.getMinutes();
    const sec = a.getSeconds() < 10 ? ('0' + a.getSeconds()) : a.getSeconds();
    return date + ' ' + month + ' ' + year + ' ' + hour + ':' + min + ':' + sec ;
  }

  updateAlbum(event) {
    const prev = this.prevAlbum;
    const albumID = this.albumService.photos[this.photo.id];
    if (albumID === undefined || albumID === "undefined" || albumID === "") {
      this.albumService.removeFromAlbum(this.photo.id, prev).subscribe(() => {}, console.error);
    } else {
      if (prev === undefined || albumID === "undefined" || albumID === "") {
        this.albumService.setPhotoAlbum(this.photo.id, albumID).subscribe(() => {}, console.error);
      } else {
        this.albumService.removeFromAlbum(this.photo.id, prev).subscribe(() => {
          this.albumService.setPhotoAlbum(this.photo.id, albumID).subscribe(() => {}, console.error);
        }, console.error);
      }
    }
  }
}

