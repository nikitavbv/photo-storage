import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { AppRoutingModule } from './app-routing.module';
import {HTTP_INTERCEPTORS, HttpClientModule} from '@angular/common/http';

import { AuthGuard } from "./_guard";
import {
  AuthenticationService,
  CryptoService,
  PageTitleService,
  PhotoService,
  SearchService,
  UserService
} from "./_services";
import { JwtInterceptor } from "./_helpers";
import { AppComponent } from './app.component';
import {AuthComponent, LandingComponent, SignInComponent} from "./landing";
import {SignUpComponent} from "./landing/auth/sign-up";
import {FormsModule} from "@angular/forms";
import {HeaderComponent, HomeComponent, PhotoComponent, PhotoModalComponent, SlideshowComponent} from "./home";

@NgModule({
  declarations: [
    AuthComponent,
    AppComponent,
    HeaderComponent,
    HomeComponent,
    LandingComponent,
    PhotoComponent,
    PhotoModalComponent,
    SlideshowComponent,
    SignInComponent,
    SignUpComponent,
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
    CryptoService,
    PageTitleService,
    PhotoService,
    SearchService,
    UserService,
    {
      provide: HTTP_INTERCEPTORS,
      useClass: JwtInterceptor,
      multi: true,
    }
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
