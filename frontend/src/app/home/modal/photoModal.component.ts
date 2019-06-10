import {Component, EventEmitter, HostListener, Input, Output} from "@angular/core";
import {Photo} from "../../_models/photo";

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
}

