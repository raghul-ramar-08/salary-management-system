import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  template: `
    <header class="app-header">
      <a routerLink="/" class="brand">ACME</a>
      <nav aria-label="Main navigation">
        <a routerLink="/" routerLinkActive="active" [routerLinkActiveOptions]="{ exact: true }">
          Employee directory
        </a>
        <a routerLink="/salary-records" routerLinkActive="active">Salary records</a>
        <a routerLink="/dashboard" routerLinkActive="active">Compensation dashboard</a>
      </nav>
      <span class="role">HR MANAGER</span>
    </header>
    <router-outlet />
  `,
  styles: [`
    .app-header {
      display: flex;
      align-items: center;
      gap: 24px;
      min-height: 58px;
      padding: 0 5vw;
      background: #173f36;
      color: #fff;
      font: 14px Arial, sans-serif;
    }
    .brand {
      color: #fff;
      font-weight: 700;
      letter-spacing: .04em;
      text-decoration: none;
    }
    .brand:hover {
      opacity: .9;
    }
    nav { display: flex; align-self: stretch; align-items: center; gap: 22px; flex: 1; margin-left: 20px; }
    nav a { display: grid; height: 100%; align-items: center; border-bottom: 2px solid transparent; color: #dce9e2; font-size: 12px; text-decoration: none; }
    nav a:hover, nav a.active { border-bottom-color: #92c7a0; color: #fff; }
    .role { color: #c8d9d1; font-size: 10px; letter-spacing: .1em; }
    @media (max-width: 600px) {
      .app-header { gap: 12px; padding: 0 14px; }
      nav { gap: 14px; margin-left: 8px; }
      .role { font-size: 9px; }
    }
  `]
})
export class AppComponent {}
