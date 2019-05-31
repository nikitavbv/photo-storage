import {Component, OnDestroy, OnInit} from "@angular/core";

@Component({
  templateUrl: 'landing.component.html',
  styleUrls: ['landing.component.less']
})
export class LandingComponent implements OnInit, OnDestroy {

  readonly SUB_HEADER_TEXTS = [
    'Secure.',
    'Stored here.',
    'Backed-up.',
    'In the cloud.',
    'Always with you.',
    'Organized.',
    'Easy to share.',
    'Without chaos.',
    'Your memories.',
    'On all your devices.'
  ];
  readonly TYPING_SPEED = 70;
  readonly ERASING_SPEED = 50;
  readonly SUBHEADER_ROTATION_INTERVAL = 5000;

  typingTimeoutID: number;
  subHeaderNextText = 'Stored here.';
  subHeaderText = '';

  ngOnInit(): void {
    this.typingTimeoutID = setTimeout(this.updateTyping.bind(this), 150);
  }

  ngOnDestroy(): void {
    clearTimeout(this.typingTimeoutID);
  }

  updateTyping(): void {
    if (this.subHeaderNextText == this.subHeaderText) {
      this.typingTimeoutID = setTimeout(this.rotateSubHeaderText.bind(this), this.SUBHEADER_ROTATION_INTERVAL);
      return;
    }

    if (this.subHeaderNextText.startsWith(this.subHeaderText)) {
      this.subHeaderText = this.subHeaderNextText.substr(0, this.subHeaderText.length + 1);
      this.typingTimeoutID = setTimeout(this.updateTyping.bind(this), this.TYPING_SPEED);
      return;
    }

    this.subHeaderText = this.subHeaderText.substr(0, this.subHeaderText.length - 1);
    this.typingTimeoutID = setTimeout(this.updateTyping.bind(this), this.ERASING_SPEED);
  }

  rotateSubHeaderText(): void {
    this.typingTimeoutID = setTimeout(this.updateTyping.bind(this), this.TYPING_SPEED);
    this.subHeaderNextText = this.SUB_HEADER_TEXTS[Math.floor(Math.random() * (this.SUB_HEADER_TEXTS.length - 1))];
  }
}

