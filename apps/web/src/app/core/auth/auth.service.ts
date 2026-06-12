import { Injectable, signal } from '@angular/core';
import { createClient, SupabaseClient, User } from '@supabase/supabase-js';
import { environment } from '../../../environments/environment';

export interface AuthState {
  user: User | null;
  loading: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private supabase: SupabaseClient;
  public authState = signal<AuthState>({ user: null, loading: true });

  constructor() {
    this.supabase = createClient(environment.supabaseUrl, environment.supabaseAnonKey);
    
    this.supabase.auth.getSession().then(({ data: { session } }) => {
      this.authState.set({ user: session?.user ?? null, loading: false });
    });

    this.supabase.auth.onAuthStateChange((_event, session) => {
      this.authState.set({ user: session?.user ?? null, loading: false });
    });
  }

  get client() {
    return this.supabase;
  }

  async getSession() {
    return this.supabase.auth.getSession();
  }

  private getDummyEmail(username: string): string {
    // Base64 encode the username to safely bypass Supabase's strict email character validation
    // while guaranteeing unique mapping without collisions.
    const safeStr = btoa(username).replace(/=/g, '');
    return `${safeStr}@omni.system`;
  }

  async signInWithUsername(username: string, password: string) {
    const email = this.getDummyEmail(username);
    return this.supabase.auth.signInWithPassword({ email, password });
  }

  async signUpWithUsername(username: string, password: string) {
    const email = this.getDummyEmail(username);
    return this.supabase.auth.signUp({ email, password });
  }

  async signOut() {
    return this.supabase.auth.signOut();
  }
}
