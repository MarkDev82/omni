import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../auth/auth.service';

export const authGuard: CanActivateFn = async () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const { data: { session } } = await authService.getSession();

  if (session) {
    return true;
  }

  return router.createUrlTree(['/login']);
};
