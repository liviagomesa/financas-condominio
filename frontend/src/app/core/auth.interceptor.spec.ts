import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from '../shared/services/auth.service';

describe('authInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let router: Router;
  let logoutCalled: boolean;
  let currentToken: string | null;

  beforeEach(() => {
    logoutCalled = false;
    currentToken = null;

    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: 'login', children: [] }]),
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        {
          provide: AuthService,
          useValue: {
            getToken: () => currentToken,
            logout: () => {
              logoutCalled = true;
            },
          },
        },
      ],
    });

    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
  });

  afterEach(() => httpMock.verify());

  it('attaches the Authorization header when a token is stored', () => {
    currentToken = 'a-jwt-token';

    http.get('/api/parties').subscribe();

    const req = httpMock.expectOne('/api/parties');
    expect(req.request.headers.get('Authorization')).toBe('Bearer a-jwt-token');
    req.flush([]);
  });

  it('does not attach the Authorization header when there is no token', () => {
    currentToken = null;

    http.get('/api/parties').subscribe();

    const req = httpMock.expectOne('/api/parties');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush([]);
  });

  it('clears the session and navigates to /login on a 401 response', () => {
    currentToken = 'a-jwt-token';
    let navigatedUrl: string | undefined;
    router.navigateByUrl = (url: unknown) => {
      navigatedUrl = String(url);
      return Promise.resolve(true);
    };

    let caughtError: unknown;
    http.get('/api/parties').subscribe({ error: (err) => (caughtError = err) });

    const req = httpMock.expectOne('/api/parties');
    req.flush({ message: 'Não autenticado.', status: 401 }, { status: 401, statusText: 'Unauthorized' });

    expect(logoutCalled).toBe(true);
    expect(navigatedUrl).toBe('/login');
    expect(caughtError).toBeTruthy();
  });

  it('forwards non-401 errors unchanged without clearing the session', () => {
    currentToken = 'a-jwt-token';

    let caughtStatus: number | undefined;
    http.get('/api/parties').subscribe({ error: (err) => (caughtStatus = err.status) });

    const req = httpMock.expectOne('/api/parties');
    req.flush({ message: 'Erro interno.', status: 500 }, { status: 500, statusText: 'Server Error' });

    expect(logoutCalled).toBe(false);
    expect(caughtStatus).toBe(500);
  });
});
