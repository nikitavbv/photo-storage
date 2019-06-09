import {Component, EventEmitter, Output, ViewChild} from "@angular/core";

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

  @ViewChild('fileInput') fileInput;

  @Output() filesToUploadSelected: EventEmitter<any> = new EventEmitter<any>();

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

  selectFilesForUpload() {
    this.fileInput.nativeElement.click();
  }

  onFilesSelected(e: Event) {
    this.filesToUploadSelected.emit((e.target as any).files);
  }
}

