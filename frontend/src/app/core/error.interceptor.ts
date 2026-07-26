import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';

export interface ApiError {
  status: number;
  message: string;
}

const FALLBACK_MESSAGE = 'Não foi possível completar a operação. Tente novamente.';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      const apiError: ApiError = {
        status: error.status,
        message: error.error?.message ?? FALLBACK_MESSAGE,
      };
      return throwError(() => apiError);
    })
  );
};
