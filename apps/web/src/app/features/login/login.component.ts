import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="login-container">
      <div class="login-card">
        <div class="brand">
          <h1>omni</h1>
          <p class="subtitle text-muted">device recovery</p>
        </div>

        <form (ngSubmit)="onSubmit()" class="login-form">
          <div class="form-group">
            <input 
              type="text" 
              [(ngModel)]="username" 
              name="username" 
              placeholder="Username" 
              autocomplete="username"
              required
              [disabled]="loading()"
            >
          </div>
          
          <div class="form-group">
            <input 
              type="password" 
              [(ngModel)]="password" 
              name="password" 
              placeholder="Password" 
              autocomplete="current-password"
              required
              [disabled]="loading()"
            >
          </div>

          <div class="error-message text-sm text-muted" *ngIf="error()">
            {{ error() }}
          </div>

          <button type="submit" class="btn-primary" [disabled]="loading() || !username || !password">
            {{ loading() ? 'Authenticating...' : 'Sign In' }}
          </button>

          <button type="button" class="btn-text text-sm text-muted" (click)="onSignUp()" [disabled]="loading() || !username || !password">
            First time? Create account
          </button>
        </form>
      </div>
    </div>
  `,
  styles: [`
    .login-container {
      display: flex;
      align-items: center;
      justify-content: center;
      min-height: 100vh;
      padding: 20px;
    }
    
    .login-card {
      background: var(--color-surface);
      padding: 48px;
      border-radius: var(--radius-lg);
      box-shadow: var(--shadow-subtle);
      width: 100%;
      max-width: 400px;
      text-align: center;
    }

    .brand {
      margin-bottom: 40px;
    }

    .brand h1 {
      font-size: 2rem;
      font-weight: 600;
      color: var(--color-text-main);
      margin-bottom: 4px;
    }

    .subtitle {
      font-size: 0.875rem;
      letter-spacing: 0.05em;
    }

    .login-form {
      display: flex;
      flex-direction: column;
      gap: 16px;
    }

    .error-message {
      color: #D32F2F;
      margin-top: -8px;
      text-align: left;
    }

    button {
      margin-top: 8px;
    }

    .btn-text {
      background: transparent;
      padding: 8px;
      margin-top: 4px;
    }

    .btn-text:hover {
      color: var(--color-text-main);
    }
  `]
})
export class LoginComponent {
  private authService = inject(AuthService);
  private router = inject(Router);

  username = '';
  password = '';
  loading = signal(false);
  error = signal('');

  async onSubmit() {
    this.loading.set(true);
    this.error.set('');

    const { error } = await this.authService.signInWithUsername(this.username, this.password);

    if (error) {
      this.error.set('Invalid credentials or account does not exist.');
      this.loading.set(false);
    } else {
      this.router.navigate(['/dashboard']);
    }
  }

  async onSignUp() {
    this.loading.set(true);
    this.error.set('');

    const { error } = await this.authService.signUpWithUsername(this.username, this.password);

    if (error) {
      this.error.set(error.message);
      this.loading.set(false);
    } else {
      this.router.navigate(['/dashboard']);
    }
  }
}
