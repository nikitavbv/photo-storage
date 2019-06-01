import {Component} from "@angular/core";

@Component({
  selector: 'auth',
  templateUrl: 'auth.component.html',
  styleUrls: ['auth.component.less'],
})
export class AuthComponent {

  readonly STATE_SIGN_IN = 'SIGN_IN';
  readonly STATE_SIGN_UP = 'SIGN_UP';

  state: string = this.STATE_SIGN_IN;

}
