import {Component, EventEmitter, HostListener, Input, Output} from "@angular/core";
import {Photo} from "../../_models/photo";

@Component({
  selector: 'photo-modal',
  templateUrl: 'photoModal.component.html',
  styleUrls: ['photoModal.component.less']
})
export class PhotoModalComponent {

  @Input() photo: Photo;
  @Output() closeModal: EventEmitter<void> = new EventEmitter();

  photoChanged: boolean = false;
  newTagName: string = '';

  @HostListener('document:keydown.escape', ['$event'])
  onKeydownHandler() {
    if (this.photoChanged) {
      this.photo.saveMetaUpdates();
    }
    this.closeModal.emit();
  }
}

