import {Component, Input} from "@angular/core";
import {Photo} from "../../_models/photo";

@Component({
  selector: 'photo',
  templateUrl: 'photo.component.html',
  styleUrls: ['photo.component.less']
})
export class PhotoComponent {

  @Input() photo: Photo;
}

