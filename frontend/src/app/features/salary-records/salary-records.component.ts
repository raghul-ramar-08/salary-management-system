import { CommonModule } from '@angular/common';
import { Component, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';
import { SalaryApiService } from './salary-api.service';
import { SalaryRecord, SalaryRecordRequest, SalaryRecordSearch } from './salary-record';
import { Router, RouterLink } from '@angular/router';

@Component({
  selector: 'app-salary-records',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatPaginatorModule,
    MatProgressBarModule,
    MatSelectModule,
    MatSortModule,
    MatTableModule,
    RouterLink
  ],
  template: `
    <main class="page-shell">
      <header class="page-heading">
        <div>
          <p class="eyebrow">COMPENSATION</p>
          <h1>Salary records</h1>
          <p class="subtitle">Browse salary history across ACME and narrow it with filters.</p>
        </div>
        <button mat-flat-button color="primary" type="button" (click)="showAddForm = !showAddForm">
          {{ showAddForm ? 'Close form' : 'Add salary record' }}
        </button>
      </header>

      <section class="panel filter-panel" aria-label="Salary record filters">
        <form class="filter-row" (ngSubmit)="fetchRecords()">
          <mat-form-field appearance="outline" class="employee-filter">
            <mat-label>Employee number</mat-label>
            <input matInput name="employeeNumber" [(ngModel)]="employeeNumber" placeholder="ACME-00001">
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Country</mat-label>
            <mat-select name="countryCode" [(ngModel)]="countryCode">
              <mat-option value="">All countries</mat-option>
              <mat-option value="US">United States</mat-option>
              <mat-option value="IN">India</mat-option>
              <mat-option value="GB">United Kingdom</mat-option>
              <mat-option value="DE">Germany</mat-option>
              <mat-option value="SG">Singapore</mat-option>
            </mat-select>
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Department</mat-label>
            <mat-select name="department" [(ngModel)]="department">
              <mat-option value="">All departments</mat-option>
              <mat-option *ngFor="let item of departments" [value]="item">{{ item }}</mat-option>
            </mat-select>
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Currency</mat-label>
            <mat-select name="currencyCode" [(ngModel)]="currencyCode">
              <mat-option value="">All currencies</mat-option>
              <mat-option *ngFor="let item of currencies" [value]="item">{{ item }}</mat-option>
            </mat-select>
          </mat-form-field>

          <mat-form-field appearance="outline" class="date-filter">
            <mat-label>Effective from</mat-label>
            <input matInput name="effectiveFrom" type="date" [(ngModel)]="effectiveFrom">
          </mat-form-field>

          <mat-form-field appearance="outline" class="date-filter">
            <mat-label>Effective to</mat-label>
            <input matInput name="effectiveTo" type="date" [(ngModel)]="effectiveTo">
          </mat-form-field>

          <label class="current-filter">
            <input type="checkbox" name="currentOnly" [(ngModel)]="currentOnly">
            Current salary only
          </label>

          <div class="filter-actions">
            <button mat-flat-button color="primary" type="submit" [disabled]="loading">
              {{ loading ? 'Fetching…' : 'Fetch records' }}
            </button>
            <button mat-button type="button" [disabled]="loading" (click)="clearFilters()">Clear</button>
          </div>
        </form>
        <p class="filter-note">Current salary periods start on or before today and end on or after today, or have no end date. Salary amounts use local currencies.</p>
      </section>

      <section class="panel add-panel" *ngIf="showAddForm">
        <h2>Add salary record</h2>
        <form class="add-form" (ngSubmit)="saveRecord()">
          <mat-form-field appearance="outline">
            <mat-label>Employee number</mat-label>
            <input matInput name="newEmployeeNumber" [(ngModel)]="draftEmployeeNumber" placeholder="ACME-00001" required>
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Annual salary</mat-label>
            <input matInput name="amount" type="number" min="0.01" step="0.01" [(ngModel)]="draft.amount" required>
          </mat-form-field>
          <mat-form-field appearance="outline" class="currency-input">
            <mat-label>Currency code</mat-label>
            <input matInput name="newCurrencyCode" maxlength="3" minlength="3" [(ngModel)]="draft.currencyCode" placeholder="USD" required>
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Effective date</mat-label>
            <input matInput name="newEffectiveDate" type="date" [(ngModel)]="draft.effectiveDate" required>
          </mat-form-field>
          <mat-form-field appearance="outline" class="reason-input">
            <mat-label>Reason</mat-label>
            <input matInput name="changeReason" maxlength="240" [(ngModel)]="draft.changeReason" placeholder="Annual review" required>
          </mat-form-field>
          <button mat-flat-button color="primary" type="submit" [disabled]="saving">
            {{ saving ? 'Saving…' : 'Save record' }}
          </button>
        </form>
      </section>

      <p *ngIf="successMessage" class="success-message" role="status">{{ successMessage }}</p>
      <p *ngIf="errorMessage" class="error-message" role="alert">{{ errorMessage }}</p>

      <section class="panel results-panel" aria-label="Salary record results">
        <mat-progress-bar *ngIf="loading" mode="indeterminate"></mat-progress-bar>
        <div *ngIf="!hasFetched && !loading" class="empty-state">
          Choose any filters, then select <strong>Fetch records</strong> to view salary data.
        </div>

        <div class="results-heading" *ngIf="hasFetched">
          <h2>Matching salary records</h2>
          <span>{{ totalRecords | number }} records</span>
        </div>
        <div class="table-scroll" *ngIf="hasFetched">
          <table mat-table [dataSource]="records" matSort
                 [matSortActive]="sortBy" [matSortDirection]="direction.toLowerCase()"
                 (matSortChange)="changeSort($event)" class="salary-table">
            <ng-container matColumnDef="employee">
              <th mat-header-cell *matHeaderCellDef mat-sort-header="employeeNumber">EMPLOYEE</th>
              <td mat-cell *matCellDef="let record">
                <a class="profile-link"
                   [routerLink]="['/employees', record.employeeNumber]"
                   [state]="{ from: 'salary-records' }"
                   (click)="goToProfile(record.employeeNumber, $event)">
                  <strong>{{ record.employeeName }}</strong>
                </a>
                <small>{{ record.employeeNumber }}</small>
              </td>
            </ng-container>
            <ng-container matColumnDef="department">
              <th mat-header-cell *matHeaderCellDef mat-sort-header="department">DEPARTMENT</th>
              <td mat-cell *matCellDef="let record">{{ record.department }}</td>
            </ng-container>
            <ng-container matColumnDef="country">
              <th mat-header-cell *matHeaderCellDef mat-sort-header="countryCode">COUNTRY</th>
              <td mat-cell *matCellDef="let record">{{ countryName(record.countryCode) }}</td>
            </ng-container>
            <ng-container matColumnDef="salary">
              <th mat-header-cell *matHeaderCellDef>ANNUAL SALARY</th>
              <td mat-cell *matCellDef="let record">{{ record.amount | number:'1.2-2' }} {{ record.currencyCode }}</td>
            </ng-container>
            <ng-container matColumnDef="effectiveDate">
              <th mat-header-cell *matHeaderCellDef mat-sort-header="effectiveDate">EFFECTIVE DATE</th>
              <td mat-cell *matCellDef="let record">{{ record.effectiveDate | date:'mediumDate' }}</td>
            </ng-container>
            <ng-container matColumnDef="effectiveTo">
              <th mat-header-cell *matHeaderCellDef>EFFECTIVE TO</th>
              <td mat-cell *matCellDef="let record">{{ record.effectiveTo ? (record.effectiveTo | date:'mediumDate') : 'Current' }}</td>
            </ng-container>
            <ng-container matColumnDef="reason">
              <th mat-header-cell *matHeaderCellDef>REASON</th>
              <td mat-cell *matCellDef="let record">{{ record.changeReason }}</td>
            </ng-container>
            <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
            <tr mat-row *matRowDef="let row; columns: displayedColumns"></tr>
            <tr class="mat-row" *matNoDataRow>
              <td class="mat-cell empty-cell" [attr.colspan]="displayedColumns.length">
                {{ errorMessage ? 'Salary records could not be loaded.' : 'No salary records match these filters.' }}
              </td>
            </tr>
          </table>
        </div>

        <mat-paginator *ngIf="hasFetched"
          [length]="totalRecords"
          [pageIndex]="pageIndex"
          [pageSize]="pageSize"
          [pageSizeOptions]="[10, 25, 50, 100]"
          [disabled]="loading"
          showFirstLastButtons
          (page)="changePage($event)">
        </mat-paginator>
      </section>
      <p class="data-note">Salary history is synthetic assessment data. Amounts remain in each employee’s local currency.</p>
    </main>
  `,
  styles: [`
    :host { display:block; min-height:100vh; background:#f4f7f5; color:#203a32; font:14px Arial,sans-serif; }
    .page-shell { max-width:1440px; margin:0 auto; padding:38px 5vw 48px; }
    .page-heading { display:flex; align-items:center; justify-content:space-between; gap:20px; margin-bottom:22px; }
    .eyebrow { margin:0 0 8px; color:#68877b; font-size:10px; font-weight:700; letter-spacing:.14em; }
    h1 { margin:0; font-size:clamp(26px,3vw,34px); letter-spacing:-.04em; }
    h2 { margin:0; font-size:16px; }
    .subtitle { margin:8px 0 0; color:#71837b; font-size:13px; }
    .panel { margin:14px 0; border:1px solid #e0e8e2; border-radius:12px; background:#fff; box-shadow:0 8px 24px #18392a0a; }
    .filter-panel { padding:18px 18px 8px; }
    .filter-row { display:flex; flex-wrap:wrap; align-items:center; gap:0 12px; }
    .filter-row mat-form-field { width:160px; }
    .filter-row .employee-filter { width:210px; }
    .filter-row .date-filter { width:165px; }
    .filter-actions { display:flex; align-items:center; gap:4px; padding-bottom:20px; }
    .current-filter { display:flex; align-items:center; gap:7px; padding:0 10px 20px 2px; color:#586b62; font-size:12px; }
    .filter-note,.data-note { margin:0 2px 8px; color:#87958e; font-size:10px; }
    .add-panel { padding:20px; }
    .add-panel h2 { margin-bottom:16px; }
    .add-form { display:flex; flex-wrap:wrap; align-items:center; gap:0 12px; }
    .add-form mat-form-field { flex:1 1 165px; }
    .add-form .currency-input { flex:0 1 145px; }
    .add-form .reason-input { flex:2 1 220px; }
    .results-panel { overflow:hidden; }
    .results-heading { display:flex; align-items:center; justify-content:space-between; padding:20px 20px 10px; }
    .results-heading span { color:#71837b; font-size:11px; }
    .table-scroll { overflow-x:auto; }
    .salary-table { width:100%; min-width:900px; }
    th.mat-mdc-header-cell { color:#7e9188; font-size:10px; font-weight:700; letter-spacing:.08em; }
    td.mat-mdc-cell, th.mat-mdc-header-cell { padding:12px 18px; border-bottom-color:#edf1ee; }
    td.mat-mdc-cell { color:#586b62; font-size:12px; }
    td strong { color:#294239; font-size:12px; font-weight:600; }
    .profile-link {
      display: inline-block;
      color: #173f36;
      text-decoration: underline;
      cursor: pointer;
    }
    .profile-link strong {
      color: #173f36;
      font-size: 12px;
      font-weight: 600;
      cursor: pointer;
    }
    .profile-link:hover strong, .profile-link:hover {
      color: #2b6146;
      text-decoration: underline;
    }
    td small { display:block; margin-top:4px; color:#899991; font-size:10px; }
    .empty-state,.empty-cell { padding:30px 16px; text-align:center; color:#7f9087; font-size:13px; }
    .success-message,.error-message { padding:0 4px; font-size:13px; }
    .success-message { color:#34714b; }.error-message { color:#a23434; }
    @media(max-width:760px) {
      .page-shell { padding:26px 14px; }
      .page-heading { align-items:flex-start; }
      .filter-row mat-form-field,.filter-row .employee-filter,.filter-row .date-filter { flex:1 1 180px; width:auto; }
      .filter-actions { padding-bottom:18px; }
    }
  `]
})
export class SalaryRecordsComponent {
  private readonly api = inject(SalaryApiService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  goToProfile(employeeNumber: string, event?: Event): void {
    if (event) {
      event.preventDefault();
      event.stopPropagation();
    }
    this.router.navigate(['/employees', employeeNumber], { state: { from: 'salary-records' } });
  }

  readonly displayedColumns = ['employee', 'department', 'country', 'salary', 'effectiveDate', 'effectiveTo', 'reason'];
  readonly departments = ['Engineering', 'Finance', 'Sales', 'People', 'Operations', 'Product'];
  readonly currencies = ['EUR', 'GBP', 'INR', 'SGD', 'USD'];
  private readonly countryNames: Record<string, string> = {
    US: 'United States', IN: 'India', GB: 'United Kingdom', DE: 'Germany', SG: 'Singapore'
  };

  employeeNumber = '';
  countryCode = '';
  department = '';
  currencyCode = '';
  effectiveFrom = '';
  effectiveTo = '';
  currentOnly = false;
  records: SalaryRecord[] = [];
  hasFetched = false;
  loading = false;
  saving = false;
  showAddForm = false;
  errorMessage = '';
  successMessage = '';
  pageIndex = 0;
  pageSize = 25;
  totalRecords = 0;
  sortBy = 'effectiveDate';
  direction: 'ASC' | 'DESC' = 'DESC';
  draftEmployeeNumber = '';
  draft: SalaryRecordRequest = { amount: 0, currencyCode: '', effectiveDate: '', changeReason: '' };

  fetchRecords(): void {
    this.pageIndex = 0;
    this.hasFetched = true;
    this.loadRecords();
  }

  clearFilters(): void {
    this.employeeNumber = '';
    this.countryCode = '';
    this.department = '';
    this.currencyCode = '';
    this.effectiveFrom = '';
    this.effectiveTo = '';
    this.currentOnly = false;
    this.records = [];
    this.totalRecords = 0;
    this.pageIndex = 0;
    this.hasFetched = false;
    this.errorMessage = '';
  }

  changePage(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadRecords();
  }

  changeSort(event: Sort): void {
    this.sortBy = event.active || 'effectiveDate';
    this.direction = event.direction === 'asc' ? 'ASC' : 'DESC';
    this.pageIndex = 0;
    this.loadRecords();
  }

  countryName(code: string): string {
    return this.countryNames[code] ?? code;
  }

  saveRecord(): void {
    if (!this.draftEmployeeNumber.trim()) return;
    this.saving = true;
    this.errorMessage = '';
    this.successMessage = '';
    const request = { ...this.draft, currencyCode: this.draft.currencyCode.trim().toUpperCase() };
    console.debug('[salary-records] Save action started', { employeeNumber: this.draftEmployeeNumber.trim() });
    this.api.add(this.draftEmployeeNumber, request).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: () => {
        this.saving = false;
        this.successMessage = 'Salary record saved.';
        this.draftEmployeeNumber = '';
        this.draft = { amount: 0, currencyCode: '', effectiveDate: '', changeReason: '' };
        if (this.hasFetched) this.loadRecords();
      },
      error: error => {
        this.saving = false;
        this.errorMessage = error.status === 409
          ? 'A salary record already exists for that employee and effective date.'
          : error.status === 404 ? 'Employee number was not found.' : 'Could not save salary record.';
      }
    });
  }

  private loadRecords(): void {
    this.loading = true;
    this.errorMessage = '';
    const criteria: SalaryRecordSearch = {
      employeeNumber: this.employeeNumber,
      countryCode: this.countryCode,
      department: this.department,
      currencyCode: this.currencyCode,
      effectiveFrom: this.effectiveFrom,
      effectiveTo: this.effectiveTo,
      currentOnly: this.currentOnly,
      page: this.pageIndex,
      size: this.pageSize,
      sortBy: this.sortBy,
      direction: this.direction
    };
    console.debug('[salary-records] Fetch action started', {
      page: this.pageIndex, filtersApplied: Boolean(this.employeeNumber.trim()
        || this.countryCode || this.department || this.currencyCode || this.effectiveFrom || this.effectiveTo
        || this.currentOnly)
    });
    this.api.search(criteria).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: page => {
        this.records = page.content;
        this.totalRecords = page.totalElements;
        this.pageIndex = page.number;
        this.loading = false;
      },
      error: error => {
        this.records = [];
        this.totalRecords = 0;
        this.loading = false;
        this.errorMessage = error.status === 400
          ? 'Check the effective date range and try again.'
          : 'Could not load salary records. Check that the backend is running.';
      }
    });
  }
}
