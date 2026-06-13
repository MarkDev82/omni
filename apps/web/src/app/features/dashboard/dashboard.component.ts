import { Component, inject, signal, effect, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { environment } from '../../../environments/environment';
import * as L from 'leaflet';

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
          <h2>{{ selectedDevice() ? 'Device Details' : 'Your Devices' }}</h2>
        </header>
        
        <div class="content-area" *ngIf="!selectedDevice()">
          <div class="empty-state" *ngIf="loadingDevices()">
             <div class="loader"></div>
             <p class="text-sm mt-4 text-muted">Loading your devices...</p>
          </div>

          <div class="empty-state" *ngIf="!loadingDevices() && !enrollmentPin() && devices().length === 0">
            <div class="card">
              <h3>No devices enrolled</h3>
              <p class="text-muted text-sm">Enroll your Android device to start tracking and recovering.</p>
              <button class="btn-primary mt-4" (click)="generatePin()" [disabled]="loadingPin()">
                {{ loadingPin() ? 'Generating...' : 'Enroll Device' }}
              </button>
            </div>
          </div>

          <div class="devices-grid" *ngIf="devices().length > 0 && !enrollmentPin()">
            <div class="card device-card clickable" *ngFor="let device of devices()" (click)="selectedDevice.set(device)">
              <div class="device-icon">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <rect x="5" y="2" width="14" height="20" rx="2" ry="2"></rect>
                  <line x1="12" y1="18" x2="12.01" y2="18"></line>
                </svg>
              </div>
              <div class="device-info">
                <h3>{{ device.model_name || 'Android Device' }}</h3>
                <p class="text-muted text-sm">Android {{ device.os_version }}</p>
                <div class="status-badge active mt-4">
                  <span class="dot"></span> Protected
                </div>
              </div>
            </div>
            
            <div class="card add-device-card" (click)="generatePin()" *ngIf="!loadingPin()">
              <div class="add-icon">+</div>
              <p>Add Device</p>
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

        <div class="content-area detail-view-area" *ngIf="selectedDevice()">
          <button class="btn-back" (click)="selectedDevice.set(null)">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <line x1="19" y1="12" x2="5" y2="12"></line>
              <polyline points="12 19 5 12 12 5"></polyline>
            </svg>
            Back to Devices
          </button>

          <div class="device-detail-header">
            <div class="device-icon large">
              <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <rect x="5" y="2" width="14" height="20" rx="2" ry="2"></rect>
                <line x1="12" y1="18" x2="12.01" y2="18"></line>
              </svg>
            </div>
            <div class="device-info-large">
              <h2>{{ selectedDevice().model_name || 'Android Device' }}</h2>
              <p class="text-muted">Android {{ selectedDevice().os_version }} &bull; Protected</p>
              <p class="text-muted text-sm mt-1">ID: {{ selectedDevice().id }}</p>
            </div>
            <div class="header-actions">
              <button class="btn-text text-sm danger-text" (click)="unlinkDevice()" *ngIf="!unlinkConfirmState()">Unlink Device</button>
              <div class="confirm-unlink" *ngIf="unlinkConfirmState()">
                <span class="text-sm">Unlink?</span>
                <button class="btn-text text-sm" (click)="unlinkConfirmState.set(false)">No</button>
                <button class="btn-text text-sm danger-text" (click)="confirmUnlink()">Yes</button>
              </div>
            </div>
          </div>

          <div class="actions-panel mt-4">
            <h3 class="mb-4">Remote Commands</h3>
            <div class="actions-grid">
              
              <div class="card action-card" (click)="sendCommand('lock')">
                <div class="action-icon">
                  <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect><path d="M7 11V7a5 5 0 0 1 10 0v4"></path></svg>
                </div>
                <div class="action-text">
                  <h4>Lock Screen</h4>
                  <p class="text-muted text-sm">Secure device immediately</p>
                </div>
                <div class="action-status" *ngIf="actionState() === 'lock'">
                  <span class="loader" *ngIf="!actionSuccess()"></span>
                  <span class="success-tick" *ngIf="actionSuccess()">✓</span>
                </div>
              </div>

              <div class="card action-card" (click)="sendCommand('alarm')">
                <div class="action-icon">
                  <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"></path><path d="M13.73 21a2 2 0 0 1-3.46 0"></path></svg>
                </div>
                <div class="action-text">
                  <h4>Toggle Alarm</h4>
                  <p class="text-muted text-sm">Start or stop ringing</p>
                </div>
                <div class="action-status" *ngIf="actionState() === 'alarm'">
                  <span class="loader" *ngIf="!actionSuccess()"></span>
                  <span class="success-tick" *ngIf="actionSuccess()">✓</span>
                </div>
              </div>

              <div class="card action-card" (click)="sendCommand('location')">
                <div class="action-icon">
                  <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"></path><circle cx="12" cy="10" r="3"></circle></svg>
                </div>
                <div class="action-text">
                  <h4>Update Location</h4>
                  <p class="text-muted text-sm">Force GPS refresh</p>
                </div>
                <div class="action-status" *ngIf="actionState() === 'location'">
                  <span class="loader" *ngIf="!actionSuccess()"></span>
                  <span class="success-tick" *ngIf="actionSuccess()">✓</span>
                </div>
              </div>

              <div class="card action-card danger" (click)="initiateWipe()" *ngIf="!wipeConfirmState()">
                <div class="action-icon">
                  <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"></polyline><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path><line x1="10" y1="11" x2="10" y2="17"></line><line x1="14" y1="11" x2="14" y2="17"></line></svg>
                </div>
                <div class="action-text">
                  <h4>Erase Data</h4>
                  <p class="text-muted text-sm">Factory reset device</p>
                </div>
              </div>

              <div class="card action-card danger confirming" *ngIf="wipeConfirmState()">
                <div class="action-text">
                  <h4>Are you sure?</h4>
                  <p class="text-sm">This cannot be undone.</p>
                </div>
                <div class="action-buttons">
                  <button class="btn-text" (click)="cancelWipe($event)">Cancel</button>
                  <button class="btn-primary danger" (click)="confirmWipe($event)">
                    <span *ngIf="actionState() !== 'wipe'">Confirm Erase</span>
                    <span class="loader white" *ngIf="actionState() === 'wipe' && !actionSuccess()"></span>
                    <span *ngIf="actionState() === 'wipe' && actionSuccess()">Erased ✓</span>
                  </button>
                </div>
              </div>

            </div>
          </div>

          <div class="card map-card mt-4">
            <h3 class="mb-4">Location Tracker</h3>
            <div id="deviceMap" style="height: 300px; border-radius: var(--radius-md); overflow: hidden; background-color: #f4f4f4;"></div>
            <p class="text-sm text-muted mt-4" *ngIf="deviceState()?.lat">Reported at: {{ deviceState()?.last_seen_at | date:'shortTime' }} on {{ deviceState()?.last_seen_at | date:'mediumDate' }}</p>
            <p class="text-sm text-muted mt-4" *ngIf="!deviceState()?.lat">Waiting for location update...</p>
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

    .devices-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
      gap: 24px;
    }

    .device-card {
      display: flex;
      flex-direction: column;
      align-items: flex-start;
    }

    .device-icon {
      background: rgba(0,0,0,0.04);
      padding: 16px;
      border-radius: var(--radius-md);
      margin-bottom: 24px;
      color: var(--color-text-main);
    }

    .status-badge {
      display: inline-flex;
      align-items: center;
      gap: 8px;
      padding: 4px 12px;
      border-radius: 100px;
      font-size: 0.85rem;
      font-weight: 500;
      background: rgba(0,0,0,0.04);
      color: var(--color-text-muted);
    }

    .status-badge.active {
      background: rgba(0, 180, 80, 0.1);
      color: rgb(0, 160, 70);
    }

    .status-badge .dot {
      width: 6px;
      height: 6px;
      border-radius: 50%;
      background: currentColor;
    }

    .add-device-card {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      border: 1px dashed var(--color-border);
      background: transparent;
      transition: all 0.2s;
      min-height: 200px;
      color: var(--color-text-muted);
    }

    .add-device-card:hover {
      border-color: var(--color-text-muted);
      color: var(--color-text-main);
      background: rgba(0,0,0,0.01);
    }

    .add-icon {
      font-size: 2rem;
      font-weight: 300;
      margin-bottom: 8px;
    }
    .btn-back {
      background: transparent;
      border: none;
      display: inline-flex;
      align-items: center;
      gap: 8px;
      color: var(--color-text-muted);
      font-weight: 500;
      cursor: pointer;
      padding: 0;
      margin-bottom: 32px;
      transition: color 0.2s;
    }

    .btn-back:hover {
      color: var(--color-text-main);
    }

    .device-detail-header {
      display: flex;
      align-items: center;
      gap: 24px;
      margin-bottom: 48px;
    }

    .device-icon.large {
      padding: 24px;
      border-radius: var(--radius-lg);
    }

    .device-info-large h2 {
      font-size: 2rem;
      font-weight: 600;
      margin-bottom: 8px;
    }

    .header-actions {
      margin-left: auto;
    }

    .danger-text {
      color: rgb(220, 50, 50);
    }
    
    .danger-text:hover {
      color: rgb(200, 40, 40);
    }

    .confirm-unlink {
      display: flex;
      align-items: center;
      gap: 8px;
      color: var(--color-text-muted);
    }

    .mb-4 { margin-bottom: 16px; }
    .mt-1 { margin-top: 4px; }

    .actions-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
      gap: 16px;
    }

    .action-card {
      display: flex;
      align-items: center;
      gap: 16px;
      padding: 24px;
      cursor: pointer;
      transition: all 0.2s;
      position: relative;
    }

    .action-card:hover {
      border-color: var(--color-text-muted);
      background: rgba(0,0,0,0.01);
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(0,0,0,0.05);
    }

    .action-icon {
      color: var(--color-text-main);
    }

    .action-card.danger:hover {
      border-color: rgba(220, 50, 50, 0.4);
      background: rgba(220, 50, 50, 0.02);
    }

    .action-card.danger .action-icon {
      color: rgb(220, 50, 50);
    }

    .action-card.confirming {
      flex-direction: column;
      align-items: flex-start;
      border-color: rgb(220, 50, 50);
      background: rgba(220, 50, 50, 0.02);
      transform: none;
      box-shadow: none;
      cursor: default;
    }

    .action-buttons {
      display: flex;
      gap: 12px;
      margin-top: 16px;
      width: 100%;
    }

    .btn-primary.danger {
      background: rgb(220, 50, 50);
    }
    
    .btn-primary.danger:hover {
      background: rgb(200, 40, 40);
    }

    .action-status {
      margin-left: auto;
      display: flex;
      align-items: center;
    }

    .loader {
      width: 18px;
      height: 18px;
      border: 2px solid rgba(0,0,0,0.1);
      border-bottom-color: var(--color-text-main);
      border-radius: 50%;
      display: inline-block;
      animation: rotation 1s linear infinite;
    }

    .loader.white {
      border: 2px solid rgba(255,255,255,0.3);
      border-bottom-color: #fff;
    }

    .success-tick {
      color: rgb(0, 160, 70);
      font-weight: bold;
      font-size: 1.2rem;
    }

    @keyframes rotation {
      0% { transform: rotate(0deg); }
      100% { transform: rotate(360deg); }
    }

    .clickable {
      cursor: pointer;
    }
    .clickable:hover {
      border-color: var(--color-text-muted);
      background: rgba(0,0,0,0.01);
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(0,0,0,0.05);
    }
  `]
})
export class DashboardComponent implements OnDestroy {
  private authService = inject(AuthService);
  private router = inject(Router);

  loadingDevices = signal<boolean>(true);
  enrollmentPin = signal<string | null>(null);
  loadingPin = signal<boolean>(false);
  devices = signal<any[]>([]);
  selectedDevice = signal<any>(null);

  actionState = signal<string | null>(null);
  actionSuccess = signal<boolean>(false);
  wipeConfirmState = signal<boolean>(false);
  unlinkConfirmState = signal<boolean>(false);
  deviceState = signal<any>(null);
  private map: L.Map | null = null;
  private marker: L.Marker | L.CircleMarker | null = null;
  private realtimeChannel: any = null;
  private devicesChannel: any = null;
  private autoTrackInterval: any = null;

  constructor() {
    effect(() => {
      const device = this.selectedDevice();
      
      if (this.autoTrackInterval) {
        clearInterval(this.autoTrackInterval);
        this.autoTrackInterval = null;
      }

      if (device) {
        this.fetchDeviceState(device.id);
        this.subscribeToDeviceUpdates(device.id);
        
        // Auto-tracking: silently refresh location every 30 seconds
        this.autoTrackInterval = setInterval(() => {
          this.sendSilentCommand('location', device.id);
        }, 30000);
      } else {
        this.deviceState.set(null);
        this.unsubscribeFromDeviceUpdates();
        if (this.map) {
          this.map.remove();
          this.map = null;
          this.marker = null;
        }
      }
    });

    effect(() => {
      const state = this.deviceState();
      // Wait for map container to render, then update
      setTimeout(() => {
        if (this.selectedDevice()) {
          this.updateMap(state?.lat, state?.lng);
        }
      }, 100);
    });
  }

  async ngOnInit() {
    await this.fetchDevices();
    this.subscribeToNewDevices();
  }

  ngOnDestroy() {
    this.unsubscribeFromDeviceUpdates();
    if (this.devicesChannel) {
      this.authService.client.removeChannel(this.devicesChannel);
    }
    if (this.autoTrackInterval) {
      clearInterval(this.autoTrackInterval);
    }
  }

  async subscribeToNewDevices() {
    const { data: { session } } = await this.authService.getSession();
    if (!session?.user?.id) return;

    this.devicesChannel = this.authService.client
      .channel('public:devices')
      .on('postgres_changes', { 
        event: 'INSERT', 
        schema: 'public', 
        table: 'devices', 
        filter: `user_id=eq.${session.user.id}` 
      }, payload => {
        console.log('New device enrolled via Realtime!', payload);
        this.enrollmentPin.set(null);
        this.fetchDevices();
      })
      .subscribe();
  }

  subscribeToDeviceUpdates(deviceId: string) {
    this.unsubscribeFromDeviceUpdates();

    this.realtimeChannel = this.authService.client
      .channel(`device_states_changes_${deviceId}`)
      .on(
        'postgres_changes',
        {
          event: '*',
          schema: 'public',
          table: 'device_states',
          filter: `device_id=eq.${deviceId}`
        },
        (payload: any) => {
          console.log('Realtime update received:', payload);
          if (payload.new && payload.new.lat) {
            this.deviceState.set(payload.new);
          }
        }
      )
      .subscribe();
  }

  unsubscribeFromDeviceUpdates() {
    if (this.realtimeChannel) {
      this.authService.client.removeChannel(this.realtimeChannel);
      this.realtimeChannel = null;
    }
  }

  async fetchDevices() {
    this.loadingDevices.set(true);
    const { data: { session } } = await this.authService.getSession();
    if (!session) {
      this.loadingDevices.set(false);
      return;
    }

    try {
      const response = await fetch(`${environment.apiUrl}/devices`, {
        headers: {
          'Authorization': `Bearer ${session.access_token}`
        }
      });
      const data = await response.json();
      if (Array.isArray(data)) {
        this.devices.set(data);
      }
    } catch (e) {
      console.error('Failed to fetch devices', e);
    } finally {
      this.loadingDevices.set(false);
    }
  }

  async fetchDeviceState(deviceId: string) {
    const { data: { session } } = await this.authService.getSession();
    if (!session) return;

    try {
      const response = await fetch(`${environment.apiUrl}/location?device_id=${deviceId}`, {
        headers: {
          'Authorization': `Bearer ${session.access_token}`
        }
      });
      const data = await response.json();
      if (data && data.lat) {
        this.deviceState.set(data);
      } else {
        this.deviceState.set(null);
      }
    } catch (e) {
      console.error('Failed to fetch device state', e);
      this.deviceState.set(null);
    }
  }

  updateMap(lat?: number, lng?: number) {
    const mapElement = document.getElementById('deviceMap');
    if (!mapElement) return;

    const hasLocation = lat !== undefined && lng !== undefined && lat !== null;

    if (!this.map) {
      this.map = L.map('deviceMap').setView(hasLocation ? [lat, lng] : [20, 0], hasLocation ? 15 : 2);
      L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png', {
        attribution: '&copy; OpenStreetMap',
        subdomains: 'abcd',
        maxZoom: 20
      }).addTo(this.map);
    }

    if (hasLocation) {
      if (this.marker) {
        this.marker.setLatLng([lat, lng]);
      } else {
        this.marker = L.circleMarker([lat, lng], {
          radius: 8,
          fillColor: '#2C2C2A',
          color: '#fff',
          weight: 2,
          opacity: 1,
          fillOpacity: 1
        }).addTo(this.map);
      }
      this.map.setView([lat, lng], 15);
    }
    
    setTimeout(() => this.map?.invalidateSize(), 100);
  }

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

  // --- MOCK REMOTE COMMANDS ---

  async sendCommand(command: string) {
    if (this.actionState()) return; // Prevent multiple clicks
    const device = this.selectedDevice();
    if (!device) return;
    
    this.actionState.set(command);
    this.actionSuccess.set(false);

    const { data: { session } } = await this.authService.getSession();
    if (!session) return;

    try {
      const response = await fetch(`${environment.apiUrl}/actions`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${session.access_token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          device_id: device.id,
          command: command
        })
      });

      if (response.ok) {
        this.actionSuccess.set(true);
      } else {
        console.error('Failed to send command:', await response.text());
      }
    } catch (e) {
      console.error('Error sending command:', e);
    } finally {
      setTimeout(() => {
        this.actionState.set(null);
        this.actionSuccess.set(false);
      }, 2000);
    }
  }

  async sendSilentCommand(command: string, deviceId: string) {
    const { data: { session } } = await this.authService.getSession();
    if (!session) return;
    try {
      await fetch(`${environment.apiUrl}/actions`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${session.access_token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ device_id: deviceId, command: command })
      });
    } catch (e) {
      console.error('Silent command failed', e);
    }
  }

  initiateWipe() {
    if (this.actionState()) return;
    this.wipeConfirmState.set(true);
  }

  cancelWipe(event: Event) {
    event.stopPropagation();
    this.wipeConfirmState.set(false);
  }

  async confirmWipe(event: Event) {
    event.stopPropagation();
    if (this.actionState()) return;
    
    this.actionState.set('wipe');
    this.actionSuccess.set(false);

    const device = this.selectedDevice();
    const { data: { session } } = await this.authService.getSession();
    
    if (device && session) {
      try {
        const response = await fetch(`${environment.apiUrl}/actions`, {
          method: 'POST',
          headers: {
            'Authorization': `Bearer ${session.access_token}`,
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({
            device_id: device.id,
            command: 'wipe'
          })
        });

        if (response.ok) {
          this.actionSuccess.set(true);
        }
      } catch (e) {
        console.error('Error sending wipe command', e);
      }
    }

    setTimeout(() => {
      this.wipeConfirmState.set(false);
      this.actionState.set(null);
      this.actionSuccess.set(false);
    }, 2000);
  }

  unlinkDevice() {
    this.unlinkConfirmState.set(true);
  }

  async confirmUnlink() {
    const device = this.selectedDevice();
    const { data: { session } } = await this.authService.getSession();

    if (device && session) {
      try {
        // Optimistic UI Update for instant feedback
        this.devices.set(this.devices().filter(d => d.id !== device.id));
        this.unlinkConfirmState.set(false);
        this.selectedDevice.set(null);

        const response = await fetch(`${environment.apiUrl}/devices?id=${device.id}`, {
          method: 'DELETE',
          headers: {
            'Authorization': `Bearer ${session.access_token}`
          }
        });

        if (!response.ok) {
          console.error('Failed to unlink device on server');
          this.fetchDevices(); // Revert on failure
        }
      } catch (e) {
        console.error('Error during unlink', e);
        this.fetchDevices(); // Revert on failure
      }
    }
  }
}
