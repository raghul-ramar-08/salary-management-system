import { CommonModule } from '@angular/common';
import { Component, DestroyRef, OnInit, inject } from '@angular/core';
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
import { RouterLink } from '@angular/router';
import { debounceTime, Subject } from 'rxjs';
import { Employee, EmployeeSearch } from './models/employee';
import { EmployeeApiService } from './services/employee-api.service';

@Component({
  selector: 'app-employee-directory',
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
          <p class="eyebrow">EMPLOYEE DATA</p>
          <h1>Employee directory</h1>
          <p class="subtitle">Search and browse employee profiles across ACME.</p>
        </div>
        <div class="headcount" aria-live="polite">
          <strong>{{ totalEmployees | number }}</strong>
          <span>employees</span>
        </div>
      </header>

      <section class="directory-card" aria-label="Employee directory">
        <div class="filter-row">
          <mat-form-field appearance="outline" class="search-field">
            <mat-label>Search name or employee number</mat-label>
            <input matInput [(ngModel)]="query" (ngModelChange)="scheduleSearch()">
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Country</mat-label>
            <mat-select [(ngModel)]="countryCode" (selectionChange)="scheduleSearch()">
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
            <mat-select [(ngModel)]="department" (selectionChange)="scheduleSearch()">
              <mat-option value="">All departments</mat-option>
              <mat-option *ngFor="let item of departments" [value]="item">{{ item }}</mat-option>
            </mat-select>
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Status</mat-label>
            <mat-select [(ngModel)]="status" (selectionChange)="scheduleSearch()">
              <mat-option value="">All statuses</mat-option>
              <mat-option value="ACTIVE">Active</mat-option>
              <mat-option value="INACTIVE">Inactive</mat-option>
            </mat-select>
          </mat-form-field>

          <button mat-button type="button" (click)="clearFilters()">Clear</button>
        </div>

        <mat-progress-bar *ngIf="loading" mode="indeterminate"></mat-progress-bar>
        <div *ngIf="errorMessage" class="error-state" role="alert">
          <span>{{ errorMessage }}</span>
          <button mat-button type="button" (click)="loadEmployees()">Retry</button>
        </div>

        <div class="table-scroll">
          <table mat-table [dataSource]="employees" matSort
                 [matSortActive]="sortBy" [matSortDirection]="direction.toLowerCase()"
                 (matSortChange)="changeSort($event)" class="employee-table">
            <ng-container matColumnDef="employee">
              <th mat-header-cell *matHeaderCellDef mat-sort-header="lastName">EMPLOYEE</th>
              <td mat-cell *matCellDef="let employee">
                <div class="employee-cell">
                  <span class="avatar">{{ employee.firstName[0] }}{{ employee.lastName[0] }}</span>
                  <span>
                  <a class="profile-link" [routerLink]="['/employees', employee.id]">
                    {{ employee.firstName }} {{ employee.lastName }}
                  </a>
                    <small>{{ employee.employeeNumber }}</small>
                  </span>
                </div>
              </td>
            </ng-container>

            <ng-container matColumnDef="role">
              <th mat-header-cell *matHeaderCellDef mat-sort-header="jobTitle">ROLE</th>
              <td mat-cell *matCellDef="let employee">
                <strong class="primary-text">{{ employee.jobTitle }}</strong>
                <small class="secondary-text">{{ employee.jobLevel || 'Level not set' }}</small>
              </td>
            </ng-container>

            <ng-container matColumnDef="department">
              <th mat-header-cell *matHeaderCellDef mat-sort-header="department">DEPARTMENT</th>
              <td mat-cell *matCellDef="let employee">{{ employee.department }}</td>
            </ng-container>

            <ng-container matColumnDef="country">
              <th mat-header-cell *matHeaderCellDef mat-sort-header="countryCode">COUNTRY</th>
              <td mat-cell *matCellDef="let employee">{{ countryName(employee.countryCode) }}</td>
            </ng-container>

            <ng-container matColumnDef="joined">
              <th mat-header-cell *matHeaderCellDef mat-sort-header="dateOfJoining">JOINED</th>
              <td mat-cell *matCellDef="let employee">{{ employee.dateOfJoining | date:'mediumDate' }}</td>
            </ng-container>

            <ng-container matColumnDef="status">
              <th mat-header-cell *matHeaderCellDef mat-sort-header="status">STATUS</th>
              <td mat-cell *matCellDef="let employee">
                <span class="status" [class.inactive]="employee.status === 'INACTIVE'">
                  {{ employee.status | titlecase }}
                </span>
              </td>
            </ng-container>

            <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
            <tr mat-row *matRowDef="let row; columns: displayedColumns"></tr>
            <tr class="mat-row" *matNoDataRow>
              <td class="mat-cell empty-cell" [attr.colspan]="displayedColumns.length">
                {{ loading ? 'Loading employees…' : 'No employees match these filters.' }}
              </td>
            </tr>
          </table>
        </div>

        <mat-paginator
          [length]="totalEmployees"
          [pageIndex]="pageIndex"
          [pageSize]="pageSize"
          [pageSizeOptions]="[10, 25, 50, 100]"
          [disabled]="loading"
          showFirstLastButtons
          (page)="changePage($event)">
        </mat-paginator>
      </section>
      <p class="data-note">Employee profiles are synthetic assessment data.</p>
    </main>
  `,
  styles: [`
    :host { display: block; min-height: 100vh; background: #f4f7f5; color: #203a32; }
    .page-shell { max-width: 1440px; margin: 0 auto; padding: 42px 5vw 48px; }
    .page-heading { display: flex; align-items: end; justify-content: space-between; margin-bottom: 26px; }
    .eyebrow { margin: 0 0 8px; color: #68877b; font-size: 10px; font-weight: 700; letter-spacing: .14em; }
    h1 { margin: 0; font-size: clamp(26px, 3vw, 34px); letter-spacing: -.04em; }
    .subtitle { margin: 8px 0 0; color: #71837b; font-size: 13px; }
    .headcount { display: grid; justify-items: end; gap: 2px; }
    .headcount strong { font-size: 23px; }
    .headcount span { color: #71837b; font-size: 11px; }
    .directory-card { overflow: hidden; border: 1px solid #e0e8e2; border-radius: 12px; background: #fff; box-shadow: 0 8px 24px #18392a0a; }
    .filter-row { display: flex; flex-wrap: wrap; align-items: center; gap: 12px; padding: 20px 20px 4px; }
    .filter-row mat-form-field { width: 170px; }
    .filter-row .search-field { width: min(320px, 100%); flex: 1 1 260px; }
    .table-scroll { overflow-x: auto; }
    .employee-table { width: 100%; min-width: 820px; }
    th.mat-mdc-header-cell { color: #7e9188; font-size: 10px; font-weight: 700; letter-spacing: .08em; }
    td.mat-mdc-cell, th.mat-mdc-header-cell { padding: 12px 18px; border-bottom-color: #edf1ee; }
    td.mat-mdc-cell { color: #586b62; font-size: 12px; }
    .employee-cell { display: flex; align-items: center; gap: 11px; min-width: 210px; }
    .avatar { display: grid; width: 34px; height: 34px; place-items: center; border-radius: 10px; background: #e7f0e9; color: #3c7156; font-size: 10px; font-weight: 700; }
    strong { color: #294239; font-size: 12px; font-weight: 600; }
    .profile-link { color: #294239; font-size: 12px; font-weight: 600; text-decoration: none; }
    .profile-link:hover { text-decoration: underline; }
    small { display: block; margin-top: 4px; color: #899991; font-size: 10px; }
    .primary-text { color: #3c554a; }
    .secondary-text { color: #899991; }
    .status { display: inline-block; border-radius: 16px; padding: 5px 9px; background: #e8f3eb; color: #417553; font-size: 10px; font-weight: 600; }
    .status.inactive { background: #f1f2ef; color: #79837d; }
    .empty-cell { height: 90px; text-align: center; color: #7f9087 !important; }
    .error-state { display: flex; align-items: center; justify-content: center; gap: 12px; padding: 20px; color: #a23434; font-size: 13px; }
    .data-note { margin: 12px 2px; color: #8a9891; font-size: 10px; }
    @media (max-width: 720px) {
      .page-shell { padding: 28px 16px; }
      .page-heading { align-items: start; }
      .filter-row { gap: 4px 10px; padding: 16px 14px 0; }
      .filter-row mat-form-field { flex: 1 1 145px; width: auto; }
      .filter-row .search-field { flex-basis: 100%; }
    }
  `]
})
export class EmployeeDirectoryComponent implements OnInit {
  private readonly api = inject(EmployeeApiService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly filterChanges = new Subject<void>();

  readonly displayedColumns = ['employee', 'role', 'department', 'country', 'joined', 'status'];
  readonly departments = ['Engineering', 'Finance', 'Sales', 'People', 'Operations', 'Product'];
  readonly countryNames: Record<string, string> = {
    US: 'United States', IN: 'India', GB: 'United Kingdom', DE: 'Germany', SG: 'Singapore'
  };

  employees: Employee[] = [];
  query = '';
  countryCode = '';
  department = '';
  status: EmployeeSearch['status'] = '';
  pageIndex = 0;
  pageSize = 25;
  totalEmployees = 0;
  sortBy = 'lastName';
  direction: 'ASC' | 'DESC' = 'ASC';
  loading = false;
  errorMessage = '';

  ngOnInit(): void {
    this.filterChanges.pipe(
      debounceTime(250),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.pageIndex = 0;
      this.loadEmployees();
    });
    this.loadEmployees();
  }

  scheduleSearch(): void {
    this.filterChanges.next();
  }

  clearFilters(): void {
    this.query = '';
    this.countryCode = '';
    this.department = '';
    this.status = '';
    this.scheduleSearch();
  }

  changePage(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadEmployees();
  }

  changeSort(event: Sort): void {
    this.sortBy = event.active || 'lastName';
    this.direction = event.direction === 'desc' ? 'DESC' : 'ASC';
    this.pageIndex = 0;
    this.loadEmployees();
  }

  loadEmployees(): void {
    this.loading = true;
    this.errorMessage = '';
    console.debug('[employee-directory] Loading employee page', { page: this.pageIndex, size: this.pageSize });
    this.api.search(this.searchCriteria()).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: page => {
        this.employees = page.content;
        this.totalEmployees = page.totalElements;
        this.pageIndex = page.number;
        this.loading = false;
        console.debug('[employee-directory] Page loaded', { returned: page.content.length, total: page.totalElements });
      },
      error: error => {
        this.employees = [];
        this.totalEmployees = 0;
        this.loading = false;
        this.errorMessage = 'Could not load employees. Check that the backend is running.';
        console.error('[employee-directory] Could not load employees', error);
      }
    });
  }

  countryName(code: string): string {
    return this.countryNames[code] ?? code;
  }

  private searchCriteria(): EmployeeSearch {
    return {
      query: this.query,
      countryCode: this.countryCode,
      department: this.department,
      status: this.status,
      page: this.pageIndex,
      size: this.pageSize,
      sortBy: this.sortBy,
      direction: this.direction
    };
  }
}
