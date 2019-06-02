import {Component, HostListener} from "@angular/core";

@Component({
  selector: 'home',
  templateUrl: 'home.component.html',
  styleUrls: ['home.component.less']
})
export class HomeComponent {

  @HostListener('dragover')
  dragover(event: any): void {
    console.log('dragover!');
  }

  @HostListener('drop')
  drop(event: any): void {
    console.log('drop');
  }

  @HostListener('dragend')
  dragend(event: any): void {
    console.log('dragend');
  }
}

