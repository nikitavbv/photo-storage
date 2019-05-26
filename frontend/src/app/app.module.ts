import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { AppRoutingModule } from './app-routing.module';
import {HTTP_INTERCEPTORS, HttpClientModule} from '@angular/common/http';

import { HeaderComponent } from "./_shared/layout";
import { AuthGuard } from "./_guard";
import { AuthenticationService, PageTitleService } from "./_services";
import { JwtInterceptor } from "./_helpers";
import { AppComponent } from './app.component';
import { LandingComponent } from "./landing";

@NgModule({
  declarations: [
    AppComponent,
    HeaderComponent,

    LandingComponent
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    HttpClientModule,
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
