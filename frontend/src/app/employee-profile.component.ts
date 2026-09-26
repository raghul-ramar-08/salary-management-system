import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, DestroyRef, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { catchError, of, switchMap, take } from 'rxjs';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { SalaryApiService } from './features/salary-records/salary-api.service';
import { SalaryRecord } from './features/salary-records/salary-record';
import { EmployeeProfile } from './models/employee';
import { EmployeeApiService, UpdateEmployeeRequest } from './services/employee-api.service';

@Component({
  selector: 'app-employee-profile',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <main class="page-shell">
      <a routerLink="/" class="back-link">← Employee directory</a>

      <p *ngIf="loading" class="state-message" role="status">Loading employee profile…</p>
      <p *ngIf="errorMessage" class="state-message error" role="alert">{{ errorMessage }}</p>

      <ng-container *ngIf="!loading && profile as detail">
        <header class="page-heading">
          <div>
            <p class="eyebrow">EMPLOYEE PROFILE</p>
            <h1>{{ detail.employee.firstName }} {{ detail.employee.lastName }}</h1>
            <p class="subtitle">{{ detail.employee.employeeNumber }} · {{ detail.employee.jobTitle }}</p>
          </div>
          <span class="status" [class.inactive]="detail.employee.status === 'INACTIVE'">
            {{ detail.employee.status | titlecase }}
          </span>
        </header>

        <section class="profile-grid" aria-label="Employee details">
          <article class="panel">
            <h2>Employment details</h2>
            <dl>
              <div><dt>Department</dt><dd>{{ detail.employee.department }}</dd></div>
              <div><dt>Country</dt><dd>{{ countryName(detail.employee.countryCode) }}</dd></div>
              <div><dt>Job level</dt><dd>{{ detail.employee.jobLevel || 'Not set' }}</dd></div>
              <div><dt>Date joined</dt><dd>{{ detail.employee.dateOfJoining | date:'mediumDate' }}</dd></div>
            </dl>
          </article>

          <article class="panel current-panel">
            <h2>Current salary</h2>
            <ng-container *ngIf="detail.currentSalary as salary; else noCurrentSalary">
              <p class="salary-amount">{{ salary.amount | number:'1.2-2' }} {{ salary.currencyCode }}</p>
              <p class="salary-period">
                Effective {{ salary.effectiveDate | date:'mediumDate' }}
                <ng-container *ngIf="salary.effectiveTo">
                  through {{ salary.effectiveTo | date:'mediumDate' }}
                </ng-container>
              </p>
              <p class="salary-reason">{{ salary.changeReason }}</p>
            </ng-container>
            <ng-template #noCurrentSalary>
              <p class="muted">No salary record is active today.</p>
            </ng-template>
          </article>
        </section>

        <section class="panel salary-change-panel" aria-label="Record salary change">
          <div class="section-heading">
            <div>
              <h2>Record salary change</h2>
              <p>Adds a new versioned salary period and closes the preceding active period automatically</p>
            </div>
          </div>
          <form class="salary-form" (ngSubmit)="submitSalaryChange(detail.employee.employeeNumber)">
            <label>
              <span>Annual amount</span>
              <input type="number" name="amount" min="0.01" step="0.01" required [(ngModel)]="newAmount" placeholder="95000.00">
            </label>
            <label>
              <span>Currency</span>
              <select name="currencyCode" required [(ngModel)]="newCurrencyCode">
                <option value="USD">USD</option>
                <option value="INR">INR</option>
                <option value="GBP">GBP</option>
                <option value="EUR">EUR</option>
                <option value="SGD">SGD</option>
              </select>
            </label>
            <label>
              <span>Effective date</span>
              <input type="date" name="effectiveDate" required [(ngModel)]="newEffectiveDate">
            </label>
            <label class="reason-field">
              <span>Change reason</span>
              <input type="text" name="changeReason" maxlength="240" required [(ngModel)]="newChangeReason" placeholder="Annual merit increase or promotion">
            </label>
            <button type="submit" [disabled]="savingSalary">
              {{ savingSalary ? 'Saving…' : 'Save salary record' }}
            </button>
          </form>
          <p *ngIf="salaryFeedback" class="feedback" [class.error]="salaryError">{{ salaryFeedback }}</p>
        </section>

        <section class="panel edit-panel" aria-label="Edit employee details">
          <div class="section-heading">
            <div>
              <h2>Edit employee details</h2>
              <p>Update profile fields — employee number cannot be changed</p>
            </div>
            <button type="button" class="toggle-btn" (click)="showEditForm = !showEditForm">
              {{ showEditForm ? 'Close' : 'Edit details' }}
            </button>
          </div>
          <form *ngIf="showEditForm" class="edit-form" (ngSubmit)="submitEdit(detail.employee.id)">
            <label>
              <span>First name</span>
              <input type="text" name="editFirstName" maxlength="80" required [(ngModel)]="editFirstName">
            </label>
            <label>
              <span>Last name</span>
              <input type="text" name="editLastName" maxlength="80" required [(ngModel)]="editLastName">
            </label>
            <label>
              <span>Country</span>
              <select name="editCountryCode" required [(ngModel)]="editCountryCode">
                <option value="US">United States (US)</option>
                <option value="IN">India (IN)</option>
                <option value="GB">United Kingdom (GB)</option>
                <option value="DE">Germany (DE)</option>
                <option value="SG">Singapore (SG)</option>
              </select>
            </label>
            <label>
              <span>Department</span>
              <select name="editDepartment" required [(ngModel)]="editDepartment">
                <option *ngFor="let d of departments" [value]="d">{{ d }}</option>
              </select>
            </label>
            <label>
              <span>Job title</span>
              <input type="text" name="editJobTitle" maxlength="120" required [(ngModel)]="editJobTitle">
            </label>
            <label>
              <span>Job level</span>
              <input type="text" name="editJobLevel" maxlength="40" [(ngModel)]="editJobLevel" placeholder="L4 (optional)">
            </label>
            <label>
              <span>Date of joining</span>
              <input type="date" name="editDateOfJoining" required [(ngModel)]="editDateOfJoining">
            </label>
            <label>
              <span>Status</span>
              <select name="editStatus" required [(ngModel)]="editStatus">
                <option value="ACTIVE">Active</option>
                <option value="INACTIVE">Inactive</option>
              </select>
            </label>
            <button type="submit" [disabled]="savingEdit">
              {{ savingEdit ? 'Saving…' : 'Save changes' }}
            </button>
          </form>
          <p *ngIf="editFeedback" class="feedback" [class.error]="editError">{{ editFeedback }}</p>
        </section>

        <section class="panel history-panel">
          <div class="section-heading">
            <div><h2>Salary history</h2><p>Past, current, and scheduled salary periods</p></div>
            <span>{{ detail.salaryHistory?.length || 0 }} records</span>
          </div>
          <div class="table-scroll" *ngIf="detail.salaryHistory && detail.salaryHistory.length; else emptyHistory">
            <table>
              <thead>
                <tr><th>ANNUAL SALARY</th><th>START DATE</th><th>END DATE</th><th>PERIOD STATUS</th><th>REASON</th></tr>
              </thead>
              <tbody>
                <tr *ngFor="let salary of detail.salaryHistory">
                  <td><strong>{{ salary.amount | number:'1.2-2' }} {{ salary.currencyCode }}</strong></td>
                  <td>{{ salary.effectiveDate | date:'mediumDate' }}</td>
                  <td>{{ salary.effectiveTo ? (salary.effectiveTo | date:'mediumDate') : 'Open-ended' }}</td>
                  <td>
                    <span class="period-badge" [class.active-badge]="isCurrentRecord(detail.currentSalary, salary)">
                      {{ periodStatusLabel(detail.currentSalary, salary) }}
                    </span>
                  </td>
                  <td>{{ salary.changeReason }}</td>
                </tr>
              </tbody>
            </table>
          </div>
          <ng-template #emptyHistory><p class="muted empty-state">No salary records are available.</p></ng-template>
        </section>
      </ng-container>
    </main>
  `,
  styles: [`
    :host { display:block; min-height:100vh; background:#f4f7f5; color:#203a32; font:14px Arial,sans-serif; }
    .page-shell { max-width:1200px; margin:0 auto; padding:38px 5vw 48px; }
    .back-link { display:inline-block; margin-bottom:24px; color:#3e745a; font-size:12px; text-decoration:none; }
    .back-link:hover { text-decoration:underline; }
    .page-heading,.section-heading { display:flex; align-items:center; justify-content:space-between; gap:18px; }
    .page-heading { margin-bottom:22px; }
    .eyebrow { margin:0 0 8px; color:#68877b; font-size:10px; font-weight:700; letter-spacing:.14em; }
    h1 { margin:0; font-size:clamp(26px,3vw,34px); letter-spacing:-.04em; }
    h2 { margin:0; font-size:16px; }
    .subtitle { margin:8px 0 0; color:#71837b; font-size:13px; }
    .profile-grid { display:grid; grid-template-columns:1fr 1fr; gap:14px; }
    .panel { margin:0 0 14px; border:1px solid #e0e8e2; border-radius:12px; background:#fff; box-shadow:0 8px 24px #18392a0a; }
    .profile-grid .panel { padding:22px; }
    dl { display:grid; grid-template-columns:1fr 1fr; gap:18px; margin:22px 0 0; }
    dt { color:#87958e; font-size:10px; text-transform:uppercase; letter-spacing:.08em; }
    dd { margin:6px 0 0; color:#294239; font-size:13px; }
    .salary-amount { margin:24px 0 4px; color:#294239; font-size:28px; font-weight:700; }
    .salary-period,.salary-reason,.muted { color:#71837b; font-size:12px; }
    .salary-reason { margin-top:20px; }
    .status { border-radius:16px; padding:7px 11px; background:#e8f3eb; color:#417553; font-size:11px; font-weight:600; }
    .status.inactive { background:#f1f2ef; color:#79837d; }
    .salary-change-panel { padding-bottom:18px; }
    .salary-form { display:grid; grid-template-columns:140px 110px 150px 1fr auto; gap:12px; align-items:end; padding:6px 22px 0; }
    .salary-form label { display:grid; gap:6px; color:#68877b; font-size:11px; font-weight:600; }
    .salary-form input,.salary-form select { height:36px; padding:0 10px; border:1px solid #d2ddd6; border-radius:8px; font-size:12px; color:#203a32; background:#fff; }
    .salary-form button { height:36px; padding:0 16px; border:0; border-radius:8px; background:#173f36; color:#fff; font-size:12px; font-weight:600; cursor:pointer; }
    .salary-form button:disabled { opacity:.6; cursor:default; }
    .feedback { margin:10px 22px 0; color:#2f6b48; font-size:12px; }
    .feedback.error { color:#a23434; }
    .edit-panel { padding-bottom:18px; }
    .section-heading { display:flex; align-items:center; justify-content:space-between; padding:20px 22px 12px; }
    .toggle-btn { height:32px; padding:0 14px; border:1px solid #b5c9be; border-radius:8px; background:#fff; color:#173f36; font-size:11px; font-weight:600; cursor:pointer; }
    .toggle-btn:hover { background:#f0f5f2; }
    .edit-form { display:grid; grid-template-columns:repeat(auto-fit, minmax(160px,1fr)); gap:12px; align-items:end; padding:6px 22px 0; }
    .edit-form label { display:grid; gap:6px; color:#68877b; font-size:11px; font-weight:600; }
    .edit-form input,.edit-form select { height:36px; padding:0 10px; border:1px solid #d2ddd6; border-radius:8px; font-size:12px; color:#203a32; background:#fff; }
    .edit-form button { height:36px; padding:0 16px; border:0; border-radius:8px; background:#173f36; color:#fff; font-size:12px; font-weight:600; cursor:pointer; align-self:end; }
    .edit-form button:disabled { opacity:.6; cursor:default; }
    .history-panel { overflow:hidden; }
    .section-heading { padding:20px 22px 12px; }
    .section-heading p { margin:6px 0 0; color:#87958e; font-size:11px; }
    .section-heading span { color:#71837b; font-size:11px; }
    .table-scroll { overflow-x:auto; }
    table { width:100%; min-width:620px; border-collapse:collapse; }
    th,td { padding:14px 18px; border-bottom:1px solid #edf1ee; text-align:left; }
    th { color:#7e9188; font-size:10px; letter-spacing:.08em; }
    td { color:#586b62; font-size:12px; }
    .period-badge { display:inline-block; border-radius:12px; padding:4px 8px; background:#f1f4f2; color:#697b72; font-size:10px; font-weight:600; }
    .period-badge.active-badge { background:#e8f3eb; color:#3c7156; }
    .empty-state,.state-message { padding:22px; }
    .error { color:#a23434; }
    @media(max-width:860px) {
      .page-shell { padding:26px 14px; }
      .profile-grid { grid-template-columns:1fr; }
      .salary-form { grid-template-columns:1fr 1fr; }
      dl { gap:14px; }
    }
  `]
})
export class EmployeeProfileComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly api = inject(EmployeeApiService);
  private readonly salaryApi = inject(SalaryApiService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly cdr = inject(ChangeDetectorRef);

  private readonly countryNames: Record<string, string> = {
    US: 'United States (US)',
    IN: 'India (IN)',
    GB: 'United Kingdom (GB)',
    DE: 'Germany (DE)',
    SG: 'Singapore (SG)'
  };
  private readonly defaultCurrencyByCountry: Record<string, string> = {
    US: 'USD',
    IN: 'INR',
    GB: 'GBP',
    DE: 'EUR',
    SG: 'SGD'
  };

  profile: EmployeeProfile | null = null;
  loading = true;
  errorMessage = '';

  newAmount: number | null = null;
  newCurrencyCode = 'USD';
  newEffectiveDate = new Date().toISOString().slice(0, 10);
  newChangeReason = '';
  savingSalary = false;
  salaryFeedback = '';
  salaryError = false;

  // Edit employee fields — pre-populated when the profile loads
  readonly departments = ['Engineering', 'Finance', 'Sales', 'People', 'Operations', 'Product'];
  showEditForm = false;
  savingEdit = false;
  editFeedback = '';
  editError = false;
  editFirstName = '';
  editLastName = '';
  editCountryCode = '';
  editDepartment = '';
  editJobTitle = '';
  editJobLevel = '';
  editDateOfJoining = '';
  editStatus: 'ACTIVE' | 'INACTIVE' = 'ACTIVE';

  private employeeIdentifier: string | number = '';

  ngOnInit(): void {
    this.route.paramMap.pipe(
      takeUntilDestroyed(this.destroyRef),
      switchMap(params => {
        const id = params.get('id');
        if (!id || !id.trim()) {
          this.loading = false;
          this.profile = null;
          this.errorMessage = 'Employee profile was not found.';
          this.cdr.markForCheck();
          return of(null);
        }
        this.employeeIdentifier = id.trim();
        this.loading = true;
        this.errorMessage = '';
        this.cdr.markForCheck();
        return this.api.profile(this.employeeIdentifier).pipe(
          catchError(error => {
            this.loading = false;
            this.errorMessage = error.status === 404
              ? 'Employee profile was not found.'
              : 'Could not load employee profile. Check that the backend is running.';
            this.cdr.markForCheck();
            return of(null);
          })
        );
      })
    ).subscribe(profile => {
      if (profile) {
        this.profile = profile;
        this.loading = false;
        this.errorMessage = '';
        this.newCurrencyCode = profile.currentSalary?.currencyCode
          ?? this.defaultCurrencyByCountry[profile.employee?.countryCode]
          ?? 'USD';
        try {
          this.populateEditForm(profile);
        } catch (e) {
          console.error('[EmployeeProfileComponent] Error populating edit form:', e);
        }
        this.cdr.markForCheck();
        this.cdr.detectChanges();
      }
    });
  }

  countryName(code: string): string {
    return this.countryNames[code] ?? code;
  }

  isCurrentRecord(current: SalaryRecord | null, candidate: SalaryRecord): boolean {
    return current !== null && current.id === candidate.id;
  }

  periodStatusLabel(current: SalaryRecord | null, candidate: SalaryRecord): string {
    if (this.isCurrentRecord(current, candidate)) {
      return 'Current';
    }
    const today = new Date().toISOString().slice(0, 10);
    return candidate.effectiveDate > today ? 'Scheduled' : 'Historical';
  }

  submitSalaryChange(employeeNumber: string): void {
    if (!this.newAmount || this.newAmount <= 0 || !this.newEffectiveDate || !this.newChangeReason.trim()) {
      this.salaryError = true;
      this.salaryFeedback = 'Provide a positive salary amount, effective date, and change reason.';
      return;
    }

    this.savingSalary = true;
    this.salaryFeedback = '';
    this.salaryError = false;
    this.cdr.markForCheck();

    this.salaryApi.add(employeeNumber, {
      amount: this.newAmount,
      currencyCode: this.newCurrencyCode,
      effectiveDate: this.newEffectiveDate,
      changeReason: this.newChangeReason.trim()
    }).pipe(take(1)).subscribe({
      next: () => {
        this.savingSalary = false;
        this.salaryFeedback = 'Salary record added and history updated.';
        this.newAmount = null;
        this.newChangeReason = '';
        this.cdr.markForCheck();
        this.loadProfile();
      },
      error: error => {
        this.savingSalary = false;
        this.salaryError = true;
        this.salaryFeedback = error?.error?.message || error?.error?.detail
          || (error.status === 409
            ? 'A salary record already exists for that effective date.'
            : 'Could not save salary record.');
        this.cdr.markForCheck();
      }
    });
  }

  private loadProfile(): void {
    this.loading = true;
    this.cdr.markForCheck();
    this.api.profile(this.employeeIdentifier).pipe(take(1)).subscribe({
      next: profile => {
        this.profile = profile;
        this.loading = false;
        this.errorMessage = '';
        this.newCurrencyCode = profile.currentSalary?.currencyCode
          ?? this.defaultCurrencyByCountry[profile.employee?.countryCode]
          ?? 'USD';
        try {
          this.populateEditForm(profile);
        } catch (e) {
          console.error('[EmployeeProfileComponent] Error populating edit form:', e);
        }
        this.cdr.markForCheck();
        this.cdr.detectChanges();
      },
      error: error => {
        this.loading = false;
        this.errorMessage = error.status === 404
          ? 'Employee profile was not found.'
          : 'Could not load employee profile. Check that the backend is running.';
        this.cdr.markForCheck();
        this.cdr.detectChanges();
      }
    });
  }

  private populateEditForm(profile: EmployeeProfile): void {
    if (!profile?.employee) return;
    const e = profile.employee;
    this.editFirstName = e.firstName || '';
    this.editLastName = e.lastName || '';
    this.editCountryCode = e.countryCode || 'US';
    this.editDepartment = e.department || (this.departments[0] ?? 'Engineering');
    this.editJobTitle = e.jobTitle || '';
    this.editJobLevel = e.jobLevel ?? '';
    this.editDateOfJoining = e.dateOfJoining || '';
    this.editStatus = e.status || 'ACTIVE';
  }

  submitEdit(employeeId: number): void {
    if (!this.editFirstName.trim() || !this.editLastName.trim() ||
        !this.editJobTitle.trim() || !this.editDateOfJoining) {
      this.editError = true;
      this.editFeedback = 'First name, last name, job title, and date of joining are required.';
      return;
    }
    this.savingEdit = true;
    this.editFeedback = '';
    this.editError = false;
    this.cdr.markForCheck();
    const request: UpdateEmployeeRequest = {
      firstName: this.editFirstName.trim(),
      lastName: this.editLastName.trim(),
      countryCode: this.editCountryCode,
      department: this.editDepartment,
      jobTitle: this.editJobTitle.trim(),
      jobLevel: this.editJobLevel.trim() || null,
      dateOfJoining: this.editDateOfJoining,
      status: this.editStatus
    };
    this.api.update(employeeId, request).pipe(take(1)).subscribe({
      next: updated => {
        this.savingEdit = false;
        this.editFeedback = 'Employee details updated successfully.';
        this.editError = false;
        this.profile = updated;
        try {
          this.populateEditForm(updated);
        } catch (e) {
          console.error(e);
        }
        this.cdr.markForCheck();
        this.cdr.detectChanges();
      },
      error: error => {
        this.savingEdit = false;
        this.editError = true;
        this.editFeedback = error?.error?.message || error?.error?.detail
          || (error.status === 400 ? 'Check all fields and try again.' : 'Could not save changes.');
        this.cdr.markForCheck();
        this.cdr.detectChanges();
      }
    });
  }
}

