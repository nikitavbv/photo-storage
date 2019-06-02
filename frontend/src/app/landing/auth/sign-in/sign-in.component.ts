import {Component} from "@angular/core";
import {AuthenticationService} from "../../../_services";

@Component({
  selector: 'sign-in',
  templateUrl: 'sign-in.component.html',
  styleUrls: ['../auth.component.less']
})
export class SignInComponent {

  username: string;
  passphrase: string;

  constructor(private auth: AuthenticationService) {}

  signIn(username: string, password: string) {
    this.auth.signIn(username, password).subscribe(console.log);
  }

}