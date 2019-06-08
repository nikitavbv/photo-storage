import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { map, catchError } from 'rxjs/operators';

@Injectable()
export class JwtInterceptor implements HttpInterceptor {

    constructor(private router: Router) {}

    intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
        const accessToken = localStorage.getItem('access_token');
        if (accessToken) {
            request = request.clone({
               setHeaders: {
                   Authorization: `Bearer ${accessToken}`
               }
            });
        }

        return next.handle(request).pipe(map(data => {
            if ('headers' in data) {
                const authHeaderValue = data.headers.get('authorization');
                if (authHeaderValue) {
                    if (authHeaderValue.startsWith('Bearer ')) {
                        const newAccessToken = authHeaderValue.substring(
                          authHeaderValue.indexOf('Bearer ') + 'Bearer '.length
                        );
                        localStorage.setItem('access_token', newAccessToken);
                    } else {
                        console.error('Unknown auth token type:', authHeaderValue);
                    }
                }
            }

            if ('body' in data) {
                const response = data as any;
                if (response.body === null) return data;

                if (response.body.status === 'auth_required') {
                    localStorage.removeItem('currentUser');
                    this.router.navigate(['/login', { returnUrl: this.router.url }]);
                    return data;
                }

                if (response.body.status === 'error') {
                    const error = response.body.error;
                    if (error === 'auth_failed' || error === 'Access Denied') {
                        localStorage.removeItem('currentUser');
                        this.router.navigate(['/login', { returnUrl: this.router.url }]);
                        return data;
                    }
                }
            }

            return data;
        })).pipe(catchError((result => {
            if (result.status === 403) {
                localStorage.removeItem('currentUser');
                this.router.navigate(['/login', { returnUrl: this.router.url }]);
            }

            return throwError(result) ;
        })));
    }

}
