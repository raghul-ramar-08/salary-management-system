import { bootstrapApplication } from '@angular/platform-browser';
import { Component } from '@angular/core';

@Component({
  selector: 'app-root',
  standalone: true,
  template: `
    <main>
      <p class="eyebrow">ACME · PEOPLE OPERATIONS</p>
      <h1>Salary Management</h1>
      <p>Frontend starter is ready. Employee and salary workflows will be added here.</p>
    </main>
  `,
  styles: [`
    :host { display: block; min-height: 100vh; background: #f5f7f5; color: #183a32; font-family: Arial, sans-serif; }
    main { max-width: 760px; margin: 12vh auto; padding: 2rem; }
    .eyebrow { color: #547766; font-size: .75rem; font-weight: 700; letter-spacing: .12em; }
    h1 { font-size: 2.5rem; margin: .5rem 0; }
    main > p:last-child { color: #65736d; }
  `]
})
class AppComponent {}

bootstrapApplication(AppComponent).catch(error => console.error(error));
