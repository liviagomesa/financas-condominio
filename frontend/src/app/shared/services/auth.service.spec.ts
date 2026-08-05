import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('starts unauthenticated when there is no stored token', () => {
    expect(service.isAuthenticated()).toBe(false);
  });

  it('stores the token and marks as authenticated on successful login', async () => {
    const result = new Promise<void>((resolve) =>
      service.login('sindica', 'senha123').subscribe(() => resolve())
    );

    const req = httpMock.expectOne('http://localhost:8082/api/auth/login');
    expect(req.request.method).toBe('POST');
    req.flush({ token: 'a-jwt-token' });

    await result;
    expect(localStorage.getItem('auth_token')).toBe('a-jwt-token');
    expect(service.isAuthenticated()).toBe(true);
  });

  it('getToken reflects the value currently stored in localStorage', () => {
    expect(service.getToken()).toBeNull();
    localStorage.setItem('auth_token', 'existing-token');
    expect(service.getToken()).toBe('existing-token');
  });

  it('logout removes the token and marks as unauthenticated', () => {
    localStorage.setItem('auth_token', 'existing-token');

    service.logout();

    expect(localStorage.getItem('auth_token')).toBeNull();
    expect(service.isAuthenticated()).toBe(false);
  });
});
