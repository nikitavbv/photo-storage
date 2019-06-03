import {Component} from "@angular/core";

@Component({
  selector: 'header',
  templateUrl: 'header.component.html',
  styleUrls: ['header.component.less']
})
export class HeaderComponent {

  readonly NOTIF_SHOW_TIME = 5000;

  notifText: string = '';
  notifCloseTimeout: number = -1;
  showingNotif: boolean = false;

  notif(text: string): void {
    this.notifText = text;
    this.showingNotif = true;
    if (this.notifCloseTimeout != -1) {
      clearTimeout(this.notifCloseTimeout);
    }
    this.notifCloseTimeout = setTimeout(() => {
      this.showingNotif = false;
    }, this.NOTIF_SHOW_TIME);
  }
}

