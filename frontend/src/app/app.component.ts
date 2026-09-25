import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  template: `
    <header class="app-header">
      <strong class="brand">ACME</strong>
      <span class="product-name">People operations</span>
      <nav aria-label="Main navigation">
        <a routerLink="/" routerLinkActive="active" [routerLinkActiveOptions]="{ exact: true }">
          Employee directory
        </a>
        <a routerLink="/salary-records" routerLinkActive="active">Salary records</a>
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
    .brand { letter-spacing: .04em; }
    .product-name { color: #c8d9d1; font-size: 13px; }
    nav { display: flex; align-self: stretch; align-items: center; gap: 22px; flex: 1; margin-left: 20px; }
    a { display: grid; height: 100%; align-items: center; border-bottom: 2px solid transparent; color: #dce9e2; font-size: 12px; text-decoration: none; }
    a:hover, a.active { border-bottom-color: #92c7a0; color: #fff; }
    .role { color: #c8d9d1; font-size: 10px; letter-spacing: .1em; }
    @media (max-width: 600px) {
      .app-header { gap: 12px; padding: 0 14px; }
      .product-name { display: none; }
      nav { gap: 14px; margin-left: 8px; }
      .role { font-size: 9px; }
    }
  `]
})
export class AppComponent {}
