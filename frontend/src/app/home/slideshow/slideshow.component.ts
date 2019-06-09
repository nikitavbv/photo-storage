import {Component, EventEmitter, HostListener, Input, Output} from "@angular/core";
import {Photo} from "../../_models/photo";

@Component({
  selector: 'slideshow',
  templateUrl: 'slideshow.component.html',
  styleUrls: ['slideshow.component.less']
})
export class SlideshowComponent {

  @Input() photo: Photo;
  @Output() closeModal: EventEmitter<void> = new EventEmitter();

  @HostListener('document:keydown.escape', ['$event'])
  onKeydownHandler() {
    this.closeModal.emit();
  }
}
