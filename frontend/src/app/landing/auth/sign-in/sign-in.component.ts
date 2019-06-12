import {Component} from "@angular/core";
import {AuthenticationService} from "../../../_services";

@Component({
  selector: 'sign-in',
  templateUrl: 'sign-in.component.html',
  styleUrls: ['../auth.component.less']
})
export class SignInComponent {

  readonly ERROR_MESSAGES = {
    'user_not_found': 'User not found',
    'password_mismatch': 'Password incorrect'
  };

  username: string;
  passphrase: string;

  errorMessage: string;

  constructor(private auth: AuthenticationService) {}

  signIn(username: string, password: string) {
    this.auth.signIn(username, password).then(() => {
      this.errorMessage = undefined;
    }, res => {
      this.errorMessage = this.ERROR_MESSAGES[res.error] || `Error: ${res.error}`;
    });
  }

}
