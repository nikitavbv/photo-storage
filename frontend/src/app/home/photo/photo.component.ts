import {Component, EventEmitter, Input, Output} from "@angular/core";
import {Photo} from "../../_models/photo";

@Component({
  selector: 'photo',
  templateUrl: 'photo.component.html',
  styleUrls: ['photo.component.less']
})
export class PhotoComponent {

  @Input() photo: Photo;
  @Output() openImageModal: EventEmitter<Photo> = new EventEmitter<Photo>();
}

