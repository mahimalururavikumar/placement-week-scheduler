import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { NotificationService } from '../services/notification.service';

export const httpErrorInterceptor: HttpInterceptorFn = (req, next) => {
  const notificationService = inject(NotificationService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      let errorTitle = 'Network / Request Failure';
      let errorMessage = 'An unknown error occurred while communicating with the server.';

      if (error.status === 0) {
        errorTitle = 'Backend Unavailable';
        errorMessage = 'Unable to connect to the scheduling backend server. Please verify the backend is running on http://localhost:8080.';
      } else if (error.error && typeof error.error === 'object' && error.error.message) {
        errorTitle = `Server Error (${error.status})`;
        errorMessage = error.error.message;
        if (error.error.details && Array.isArray(error.error.details) && error.error.details.length > 0) {
          errorMessage += `: ${error.error.details.join(', ')}`;
        }
      } else if (typeof error.error === 'string') {
        errorTitle = `Error (${error.status})`;
        errorMessage = error.error;
      } else if (error.status === 404) {
        errorTitle = 'Resource Not Found (404)';
        errorMessage = 'The requested resource or endpoint was not found.';
      } else if (error.status === 400) {
        errorTitle = 'Invalid Request (400)';
        errorMessage = error.statusText || 'Bad Request payload or parameters.';
      } else if (error.status >= 500) {
        errorTitle = 'Internal Server Error (500)';
        errorMessage = 'An internal server error occurred on the scheduling backend.';
      }

      console.error(`[HttpErrorInterceptor] ${errorTitle}:`, error);
      notificationService.showError(errorTitle, errorMessage);

      return throwError(() => error);
    })
  );
};
