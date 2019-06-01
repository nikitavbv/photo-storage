import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { AppRoutingModule } from './app-routing.module';
import {HTTP_INTERCEPTORS, HttpClientModule} from '@angular/common/http';

import { HeaderComponent } from "./_shared/layout";
import { AuthGuard } from "./_guard";
import { AuthenticationService, PageTitleService } from "./_services";
import { JwtInterceptor } from "./_helpers";
import { AppComponent } from './app.component';
import {AuthComponent, LandingComponent, SignInComponent} from "./landing";
import {SignUpComponent} from "./landing/auth/sign-up/sign-up.component";
import {FormsModule} from "@angular/forms";
import {HomeComponent} from "./home";

@NgModule({
  declarations: [
    AuthComponent,
    AppComponent,
    HeaderComponent,
    HomeComponent,
    LandingComponent,
    SignInComponent,
    SignUpComponent
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    HttpClientModule,
    FormsModule,
  ],
  providers: [
    AuthGuard,
    AuthenticationService,
    PageTitleService,
    {
      provide: HTTP_INTERCEPTORS,
      useClass: JwtInterceptor,
      multi: true,
    }
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
