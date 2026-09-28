import { HttpInterceptorFn, HttpResponse, HttpErrorResponse } from '@angular/common/http';
import { tap } from 'rxjs';

export const loggingInterceptor: HttpInterceptorFn = (req, next) => {
  const started = Date.now();
  console.debug(`[HTTP REQ] ${req.method} ${req.urlWithParams}`);

  return next(req).pipe(
    tap({
      next: (event) => {
        if (event instanceof HttpResponse) {
          const elapsed = Date.now() - started;
          console.debug(`[HTTP RES] ${req.method} ${req.urlWithParams} -> ${event.status} (${elapsed}ms)`);
        }
      },
      error: (error: HttpErrorResponse) => {
        const elapsed = Date.now() - started;
        console.error(`[HTTP ERR] ${req.method} ${req.urlWithParams} -> ${error.status} ${error.statusText} (${elapsed}ms)`);
      }
    })
  );
};
