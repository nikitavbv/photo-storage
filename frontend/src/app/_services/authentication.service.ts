import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';

@Injectable()
export class AuthenticationService {
    constructor(private http: HttpClient) {}

    login() {
        // TODO: implement this
    }

    logout() {
        // remove user from local storage to log user out
        localStorage.removeItem('currentUser');
    }

    isLoggedIn() {
        return localStorage && localStorage.getItem('currentUser');
    }

    getUser() {
        return JSON.parse(localStorage.getItem('currentUser'));
    }
}