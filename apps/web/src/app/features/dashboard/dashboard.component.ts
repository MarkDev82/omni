import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="dashboard-layout">
      <aside class="sidebar">
        <div class="brand">
          <h1>omni</h1>
        </div>
        <nav class="nav-links">
          <a href="#" class="active">Devices</a>
          <a href="#">Settings</a>
        </nav>
        <div class="sidebar-footer">
          <button class="btn-logout text-sm text-muted" (click)="logout()">Sign out</button>
        </div>
      </aside>
      
      <main class="main-content">
        <header class="topbar">
          <h2>Your Devices</h2>
        </header>
        
        <div class="content-area">
          <div class="empty-state" *ngIf="!enrollmentPin()">
            <div class="card">
              <h3>No devices enrolled</h3>
              <p class="text-muted text-sm">Enroll your Android device to start tracking and recovering.</p>
              <button class="btn-primary mt-4" (click)="generatePin()" [disabled]="loadingPin()">
                {{ loadingPin() ? 'Generating...' : 'Enroll Device' }}
              </button>
            </div>
          </div>

          <div class="enrollment-state" *ngIf="enrollmentPin()">
            <div class="card pin-card">
              <h3>Enrollment Code</h3>
              <p class="text-muted text-sm">Enter this 6-digit code in the Omni Android app to link your device.</p>
              <div class="pin-display">
                {{ enrollmentPin() }}
              </div>
              <p class="text-xs text-muted mt-4">This code expires in 15 minutes.</p>
              <button class="btn-text mt-4" (click)="enrollmentPin.set(null)">Cancel</button>
            </div>
          </div>
        </div>
      </main>
    </div>
  `,
  styles: [`
    .dashboard-layout {
      display: flex;
      height: 100vh;
      overflow: hidden;
    }

    .sidebar {
      width: 240px;
      border-right: 1px solid var(--color-border);
      background: var(--color-surface);
      display: flex;
      flex-direction: column;
      padding: 32px 24px;
    }

    .brand h1 {
      font-size: 1.5rem;
      font-weight: 600;
      margin-bottom: 48px;
    }

    .nav-links {
      display: flex;
      flex-direction: column;
      gap: 12px;
      flex-grow: 1;
    }

    .nav-links a {
      text-decoration: none;
      color: var(--color-text-muted);
      font-weight: 500;
      padding: 8px 12px;
      border-radius: var(--radius-md);
      transition: all 0.2s;
    }

    .nav-links a:hover {
      color: var(--color-text-main);
      background: rgba(0,0,0,0.03);
    }

    .nav-links a.active {
      color: var(--color-text-main);
      background: rgba(0,0,0,0.05);
    }

    .btn-logout {
      background: transparent;
      text-align: left;
      padding: 8px 12px;
      width: 100%;
    }

    .btn-logout:hover {
      color: var(--color-text-main);
    }

    .main-content {
      flex-grow: 1;
      display: flex;
      flex-direction: column;
      background: var(--color-bg);
    }

    .topbar {
      padding: 32px 48px 16px;
    }

    .topbar h2 {
      font-size: 1.25rem;
      font-weight: 500;
    }

    .content-area {
      padding: 16px 48px;
      flex-grow: 1;
      overflow-y: auto;
    }

    .card {
      background: var(--color-surface);
      padding: 32px;
      border-radius: var(--radius-lg);
      box-shadow: var(--shadow-subtle);
      border: 1px solid var(--color-border);
    }

    .mt-4 { margin-top: 16px; }

    .pin-display {
      font-size: 2.5rem;
      font-weight: 600;
      letter-spacing: 0.2em;
      text-align: center;
      margin: 24px 0;
      padding: 24px;
      background: var(--color-bg);
      border-radius: var(--radius-md);
      border: 1px solid var(--color-border);
      color: var(--color-text-main);
    }

    .pin-card {
      max-width: 400px;
      text-align: center;
    }

    .btn-text {
      background: transparent;
      padding: 8px 16px;
      font-weight: 500;
      color: var(--color-text-muted);
    }
    
    .btn-text:hover { color: var(--color-text-main); }
  `]
})
export class DashboardComponent {
  private authService = inject(AuthService);
  private router = inject(Router);

  enrollmentPin = signal<string | null>(null);
  loadingPin = signal<boolean>(false);

  async generatePin() {
    this.loadingPin.set(true);
    const { data: { session } } = await this.authService.getSession();
    
    if (!session) {
      this.loadingPin.set(false);
      return;
    }

    try {
      const response = await fetch(`${environment.apiUrl}/enrollment/generate`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${session.access_token}`
        }
      });
      const data = await response.json();
      if (data.pin) {
        this.enrollmentPin.set(data.pin);
      }
    } catch (e) {
      console.error('Failed to generate PIN', e);
    } finally {
      this.loadingPin.set(false);
    }
  }

  async logout() {
    await this.authService.signOut();
    this.router.navigate(['/login']);
  }
}
