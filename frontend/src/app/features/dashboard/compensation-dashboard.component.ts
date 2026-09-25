import { CommonModule } from '@angular/common';
import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import {
  CountrySalaryReportResponse,
  DepartmentSalaryExtremesReportResponse,
  DepartmentSalaryReportResponse,
  SalaryDistributionReportResponse
} from '../../models/reports';
import { ReportApiService } from '../../services/report-api.service';

@Component({
  selector: 'app-compensation-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <main class="page-shell">
      <header class="page-heading">
        <div>
          <p class="eyebrow">COMPENSATION ANALYTICS</p>
          <h1>Organization compensation dashboard</h1>
          <p class="subtitle">
            Cross-country payroll, median/average benchmarks, salary band distributions, and departmental pay extremes.
          </p>
        </div>
        <div class="controls-card" aria-label="Report parameters">
          <label>
            <span>As-of date</span>
            <input type="date" name="asOfDate" [(ngModel)]="asOfDate" (change)="loadAllReports()">
          </label>
          <label>
            <span>Band width (USD)</span>
            <select name="bandSize" [(ngModel)]="bandSize" (ngModelChange)="loadDistributionOnly()">
              <option [ngValue]="15000">$15,000 bands</option>
              <option [ngValue]="25000">$25,000 bands</option>
              <option [ngValue]="40000">$40,000 bands</option>
              <option [ngValue]="50000">$50,000 bands</option>
            </select>
          </label>
          <label class="checkbox-label">
            <input type="checkbox" name="includeInactive" [(ngModel)]="includeInactive" (change)="loadAllReports()">
            <span>Include inactive employees</span>
          </label>
        </div>
      </header>

      <section class="disclosure-banner" *ngIf="countryReport as summary" aria-label="Currency and eligibility rules">
        <div>
          <strong>Reporting Currency: {{ summary.reportingCurrency }} (Normalized)</strong>
          <p>{{ summary.rateBasis }}</p>
        </div>
        <div class="rate-chips">
          <span class="rate-chip" *ngFor="let item of exchangeRateEntries()">
            1 {{ item[0] }} = {{ item[1] | number:'1.2-4' }} {{ summary.reportingCurrency }}
          </span>
        </div>
      </section>

      <p *ngIf="loading" class="state-message" role="status">Loading compensation analytics…</p>
      <div *ngIf="errorMessage" class="state-message error" role="alert">
        <span>{{ errorMessage }}</span>
        <button type="button" (click)="loadAllReports()">Retry</button>
      </div>

      <ng-container *ngIf="countryReport as cReport">
        <section class="kpi-grid" aria-label="Organization summary">
          <article class="kpi-card">
            <span>INCLUDED HEADCOUNT</span>
            <strong>{{ cReport.totalHeadcount | number }}</strong>
            <small>{{ includeInactive ? 'Active + inactive employees' : 'Active employees as of ' + cReport.asOfDate }}</small>
          </article>
          <article class="kpi-card">
            <span>TOTAL ANNUAL PAYROLL ({{ cReport.reportingCurrency }})</span>
            <strong>\${{ cReport.totalPayrollReportingCurrency | number:'1.2-2' }}</strong>
            <small>Normalized across {{ cReport.countries.length }} countries</small>
          </article>
          <article class="kpi-card">
            <span>ORG AVERAGE SALARY ({{ cReport.reportingCurrency }})</span>
            <strong>\${{ cReport.overallAverageReportingCurrency | number:'1.2-2' }}</strong>
            <small>Mean annual compensation</small>
          </article>
          <article class="kpi-card">
            <span>ORG MEDIAN SALARY ({{ cReport.reportingCurrency }})</span>
            <strong>\${{ cReport.overallMedianReportingCurrency | number:'1.2-2' }}</strong>
            <small>50th percentile annual pay</small>
          </article>
        </section>

        <div class="two-col-grid">
          <section class="panel" aria-label="Country salary report">
            <div class="section-heading">
              <div>
                <h2>Pay &amp; payroll by country</h2>
                <p>Shows both local currency metrics and normalized {{ cReport.reportingCurrency }} equivalents</p>
              </div>
            </div>
            <div class="table-scroll">
              <table>
                <thead>
                  <tr>
                    <th>COUNTRY</th>
                    <th>HEADCOUNT</th>
                    <th>LOCAL AVG / MEDIAN</th>
                    <th>NORMALIZED AVG ({{ cReport.reportingCurrency }})</th>
                    <th>NORMALIZED MEDIAN ({{ cReport.reportingCurrency }})</th>
                    <th>TOTAL PAYROLL ({{ cReport.reportingCurrency }})</th>
                  </tr>
                </thead>
                <tbody>
                  <tr *ngFor="let row of cReport.countries">
                    <td><strong>{{ countryName(row.countryCode) }}</strong></td>
                    <td>{{ row.headcount | number }}</td>
                    <td>
                      <ng-container *ngIf="row.averageSalaryLocal !== null; else mixedCurrency">
                        {{ row.averageSalaryLocal | number:'1.0-0' }} / {{ row.medianSalaryLocal | number:'1.0-0' }} {{ row.localCurrencyCode }}
                      </ng-container>
                      <ng-template #mixedCurrency>Mixed currencies</ng-template>
                    </td>
                    <td>\${{ row.averageSalaryReportingCurrency | number:'1.2-2' }}</td>
                    <td>\${{ row.medianSalaryReportingCurrency | number:'1.2-2' }}</td>
                    <td>
                      <div class="payroll-cell">
                        <strong>\${{ row.totalPayrollReportingCurrency | number:'1.2-2' }}</strong>
                        <div class="mini-bar">
                          <span [style.width.%]="payrollShare(row.totalPayrollReportingCurrency, cReport.totalPayrollReportingCurrency)"></span>
                        </div>
                      </div>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </section>

          <section class="panel" *ngIf="departmentReport as dReport" aria-label="Department salary report">
            <div class="section-heading">
              <div>
                <h2>Pay &amp; payroll by department</h2>
                <p>Multi-country teams normalized to {{ dReport.reportingCurrency }}</p>
              </div>
            </div>
            <div class="table-scroll">
              <table>
                <thead>
                  <tr>
                    <th>DEPARTMENT</th>
                    <th>HEADCOUNT</th>
                    <th>AVERAGE ({{ dReport.reportingCurrency }})</th>
                    <th>MEDIAN ({{ dReport.reportingCurrency }})</th>
                    <th>TOTAL PAYROLL ({{ dReport.reportingCurrency }})</th>
                  </tr>
                </thead>
                <tbody>
                  <tr *ngFor="let dept of dReport.departments">
                    <td><strong>{{ dept.department }}</strong></td>
                    <td>{{ dept.headcount | number }}</td>
                    <td>\${{ dept.averageSalaryReportingCurrency | number:'1.2-2' }}</td>
                    <td>\${{ dept.medianSalaryReportingCurrency | number:'1.2-2' }}</td>
                    <td>
                      <div class="payroll-cell">
                        <strong>\${{ dept.totalPayrollReportingCurrency | number:'1.2-2' }}</strong>
                        <div class="mini-bar">
                          <span [style.width.%]="payrollShare(dept.totalPayrollReportingCurrency, dReport.totalPayrollReportingCurrency)"></span>
                        </div>
                      </div>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </section>
        </div>
      </ng-container>

      <section class="panel" *ngIf="distributionReport as dist" aria-label="Salary band distribution">
        <div class="section-heading">
          <div>
            <h2>Salary distribution by band ({{ dist.reportingCurrency }})</h2>
            <p>Organization-wide and per-country distribution across \${{ dist.bandSize | number:'1.0-0' }} bands</p>
          </div>
          <label class="inline-filter">
            <span>Inspect country:</span>
            <select [(ngModel)]="selectedDistributionCountry">
              <option value="ALL">Organization-wide ({{ dist.totalHeadcount | number }} employees)</option>
              <option *ngFor="let c of dist.countryDistributions" [value]="c.countryCode">
                {{ countryName(c.countryCode) }} ({{ c.totalHeadcount | number }} employees)
              </option>
            </select>
          </label>
        </div>

        <div class="histogram-list">
          <div class="histogram-row" *ngFor="let bucket of activeDistributionBands()">
            <div class="band-label">{{ bucket.label }}</div>
            <div class="bar-track">
              <div class="bar-fill" [style.width.%]="bucket.percentageOfTotal"></div>
            </div>
            <div class="band-stats">
              <strong>{{ bucket.headcount | number }}</strong>
              <span>({{ bucket.percentageOfTotal | number:'1.1-1' }}%)</span>
            </div>
          </div>
        </div>

        <div class="country-matrix-wrap" *ngIf="dist.countryDistributions.length">
          <h3>Country band comparison matrix</h3>
          <div class="table-scroll">
            <table>
              <thead>
                <tr>
                  <th>COUNTRY</th>
                  <th>TOTAL</th>
                  <th *ngFor="let band of dist.organizationBands">{{ band.label }}</th>
                </tr>
              </thead>
              <tbody>
                <tr *ngFor="let countryDist of dist.countryDistributions">
                  <td><strong>{{ countryName(countryDist.countryCode) }}</strong></td>
                  <td>{{ countryDist.totalHeadcount | number }}</td>
                  <td *ngFor="let band of countryDist.bands">
                    {{ band.headcount | number }}
                    <small class="pct-note">({{ band.percentageOfTotal | number:'1.0-1' }}%)</small>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </section>

      <section class="panel" *ngIf="extremesReport as ext" aria-label="Highest and lowest paid employees by department">
        <div class="section-heading">
          <div>
            <h2>Highest- and lowest-paid employees by department</h2>
            <p>Ranked by normalized {{ ext.reportingCurrency }} annual compensation as of {{ ext.asOfDate }}</p>
          </div>
        </div>
        <div class="extremes-grid">
          <article class="extreme-card" *ngFor="let dept of ext.departments">
            <header>
              <h3>{{ dept.department }}</h3>
              <span>{{ dept.headcount | number }} active employees</span>
            </header>
            <div class="outlier-columns">
              <div class="outlier-box highest" *ngIf="dept.highestPaid as high">
                <span class="badge high-badge">HIGHEST PAID</span>
                <a class="emp-link" [routerLink]="['/employees', high.employeeId]">
                  {{ high.fullName }} ({{ high.employeeNumber }})
                </a>
                <p class="role-meta">{{ high.jobTitle }} · {{ high.jobLevel || 'Level N/A' }} · {{ high.countryCode }}</p>
                <p class="pay-highlight">\${{ high.reportingCurrencyAmount | number:'1.2-2' }} {{ ext.reportingCurrency }}</p>
                <small>{{ high.localAmount | number:'1.2-2' }} {{ high.localCurrencyCode }} (effective {{ high.effectiveDate }})</small>
              </div>
              <div class="outlier-box lowest" *ngIf="dept.lowestPaid as low">
                <span class="badge low-badge">LOWEST PAID</span>
                <a class="emp-link" [routerLink]="['/employees', low.employeeId]">
                  {{ low.fullName }} ({{ low.employeeNumber }})
                </a>
                <p class="role-meta">{{ low.jobTitle }} · {{ low.jobLevel || 'Level N/A' }} · {{ low.countryCode }}</p>
                <p class="pay-highlight">\${{ low.reportingCurrencyAmount | number:'1.2-2' }} {{ ext.reportingCurrency }}</p>
                <small>{{ low.localAmount | number:'1.2-2' }} {{ low.localCurrencyCode }} (effective {{ low.effectiveDate }})</small>
              </div>
            </div>
          </article>
        </div>
      </section>
    </main>
  `,
  styles: [`
    :host { display:block; min-height:100vh; background:#f4f7f5; color:#203a32; font:14px Arial,sans-serif; }
    .page-shell { max-width:1440px; margin:0 auto; padding:38px 5vw 54px; }
    .page-heading { display:flex; flex-wrap:wrap; align-items:end; justify-content:space-between; gap:18px; margin-bottom:20px; }
    .eyebrow { margin:0 0 8px; color:#68877b; font-size:10px; font-weight:700; letter-spacing:.14em; }
    h1 { margin:0; font-size:clamp(26px,3vw,34px); letter-spacing:-.04em; }
    h2 { margin:0; font-size:16px; }
    h3 { margin:0; font-size:14px; color:#294239; }
    .subtitle { margin:8px 0 0; color:#71837b; font-size:13px; }
    .controls-card { display:flex; flex-wrap:wrap; align-items:end; gap:14px; padding:14px 16px; border:1px solid #dce6df; border-radius:12px; background:#fff; }
    .controls-card label { display:grid; gap:5px; color:#68877b; font-size:11px; font-weight:600; }
    .controls-card input[type="date"], .controls-card select, .inline-filter select {
      height:34px; padding:0 10px; border:1px solid #d2ddd6; border-radius:8px; font-size:12px; color:#203a32; background:#fff;
    }
    .checkbox-label { display:flex !important; align-items:center; gap:8px !important; height:34px; cursor:pointer; }
    .disclosure-banner {
      display:flex; flex-wrap:wrap; align-items:center; justify-content:space-between; gap:14px;
      margin-bottom:20px; padding:14px 18px; border:1px solid #cce0d3; border-radius:12px; background:#ebf4ee;
    }
    .disclosure-banner strong { font-size:12px; color:#1d4736; }
    .disclosure-banner p { margin:4px 0 0; color:#49695b; font-size:11px; }
    .rate-chips { display:flex; flex-wrap:wrap; gap:8px; }
    .rate-chip { padding:4px 9px; border-radius:14px; background:#fff; border:1px solid #cde0d4; color:#2a5442; font-size:11px; font-weight:600; }
    .kpi-grid { display:grid; grid-template-columns:repeat(4,1fr); gap:14px; margin-bottom:18px; }
    .kpi-card { padding:18px 20px; border:1px solid #e0e8e2; border-radius:12px; background:#fff; box-shadow:0 8px 24px #18392a0a; }
    .kpi-card span { color:#7a8e84; font-size:10px; font-weight:700; letter-spacing:.08em; }
    .kpi-card strong { display:block; margin:8px 0 4px; color:#1d3b31; font-size:24px; }
    .kpi-card small { color:#7c8e85; font-size:11px; }
    .two-col-grid { display:grid; grid-template-columns:1fr 1fr; gap:16px; margin-bottom:18px; }
    .panel { margin-bottom:18px; border:1px solid #e0e8e2; border-radius:12px; background:#fff; box-shadow:0 8px 24px #18392a0a; overflow:hidden; }
    .section-heading { display:flex; flex-wrap:wrap; align-items:center; justify-content:space-between; gap:14px; padding:18px 20px 12px; }
    .section-heading p { margin:4px 0 0; color:#7b8d84; font-size:11px; }
    .inline-filter { display:flex; align-items:center; gap:8px; font-size:12px; color:#5a6e64; font-weight:600; }
    .table-scroll { overflow-x:auto; }
    table { width:100%; border-collapse:collapse; }
    th,td { padding:12px 16px; border-bottom:1px solid #edf1ee; text-align:left; font-size:12px; }
    th { color:#7e9188; font-size:10px; font-weight:700; letter-spacing:.08em; }
    .payroll-cell { display:grid; gap:5px; }
    .mini-bar { height:5px; width:110px; border-radius:4px; background:#ebf0ed; overflow:hidden; }
    .mini-bar span { display:block; height:100%; background:#3c7156; }
    .histogram-list { display:grid; gap:10px; padding:8px 20px 18px; }
    .histogram-row { display:grid; grid-template-columns:150px 1fr 110px; align-items:center; gap:14px; }
    .band-label { font-size:12px; font-weight:600; color:#314b40; }
    .bar-track { height:16px; border-radius:8px; background:#edf2ef; overflow:hidden; }
    .bar-fill { height:100%; border-radius:8px; background:linear-gradient(90deg,#265c47,#5ca37d); }
    .band-stats { text-align:right; font-size:12px; }
    .band-stats span { margin-left:5px; color:#7c8f85; font-size:11px; }
    .country-matrix-wrap { padding:8px 20px 18px; border-top:1px solid #edf1ee; }
    .country-matrix-wrap h3 { margin:8px 0 12px; }
    .pct-note { display:inline; margin-left:4px; color:#82948b; font-size:10px; }
    .extremes-grid { display:grid; grid-template-columns:repeat(auto-fit,minmax(360px,1fr)); gap:14px; padding:8px 20px 20px; }
    .extreme-card { padding:16px; border:1px solid #e3ebe5; border-radius:10px; background:#fafcfb; }
    .extreme-card header { display:flex; align-items:center; justify-content:space-between; margin-bottom:12px; }
    .extreme-card header span { color:#7b8e85; font-size:11px; }
    .outlier-columns { display:grid; grid-template-columns:1fr 1fr; gap:10px; }
    .outlier-box { padding:12px; border-radius:8px; background:#fff; border:1px solid #e6eee8; }
    .badge { display:inline-block; margin-bottom:6px; padding:3px 7px; border-radius:10px; font-size:9px; font-weight:700; letter-spacing:.06em; }
    .high-badge { background:#e6f3ea; color:#2e6b48; }
    .low-badge { background:#f3efe6; color:#7b5c29; }
    .emp-link { display:block; color:#1d4234; font-size:12px; font-weight:700; text-decoration:none; }
    .emp-link:hover { text-decoration:underline; }
    .role-meta { margin:4px 0 8px; color:#778a80; font-size:11px; }
    .pay-highlight { margin:0 0 2px; color:#203a32; font-size:15px; font-weight:700; }
    .outlier-box small { color:#7e9087; font-size:10px; }
    .state-message { padding:20px; border-radius:10px; background:#fff; }
    .state-message.error { display:flex; align-items:center; justify-content:space-between; color:#a23434; }
    @media(max-width:1050px) {
      .kpi-grid { grid-template-columns:1fr 1fr; }
      .two-col-grid { grid-template-columns:1fr; }
    }
    @media(max-width:640px) {
      .kpi-grid { grid-template-columns:1fr; }
      .histogram-row { grid-template-columns:115px 1fr 85px; }
      .outlier-columns { grid-template-columns:1fr; }
    }
  `]
})
export class CompensationDashboardComponent implements OnInit {
  private readonly reportApi = inject(ReportApiService);
  private readonly destroyRef = inject(DestroyRef);

  private readonly countryNames: Record<string, string> = {
    US: 'United States (US)',
    IN: 'India (IN)',
    GB: 'United Kingdom (GB)',
    DE: 'Germany (DE)',
    SG: 'Singapore (SG)'
  };

  asOfDate = new Date().toISOString().slice(0, 10);
  includeInactive = false;
  bandSize = 25000;
  selectedDistributionCountry = 'ALL';

  countryReport: CountrySalaryReportResponse | null = null;
  departmentReport: DepartmentSalaryReportResponse | null = null;
  distributionReport: SalaryDistributionReportResponse | null = null;
  extremesReport: DepartmentSalaryExtremesReportResponse | null = null;

  loading = false;
  errorMessage = '';

  ngOnInit(): void {
    this.loadAllReports();
  }

  loadAllReports(): void {
    this.loading = true;
    this.errorMessage = '';

    forkJoin({
      countries: this.reportApi.countryReport(this.asOfDate, this.includeInactive),
      departments: this.reportApi.departmentReport(this.asOfDate, this.includeInactive),
      distribution: this.reportApi.distributionReport(this.asOfDate, this.includeInactive, this.bandSize),
      extremes: this.reportApi.departmentExtremesReport(this.asOfDate, this.includeInactive, 3)
    }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: res => {
        this.countryReport = res.countries;
        this.departmentReport = res.departments;
        this.distributionReport = res.distribution;
        this.extremesReport = res.extremes;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.errorMessage = 'Could not load compensation reports. Check that the backend is running.';
      }
    });
  }

  loadDistributionOnly(): void {
    this.reportApi.distributionReport(this.asOfDate, this.includeInactive, this.bandSize)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: distribution => {
          this.distributionReport = distribution;
        }
      });
  }

  exchangeRateEntries(): [string, number][] {
    if (!this.countryReport) {
      return [];
    }
    return Object.entries(this.countryReport.exchangeRatesToReportingCurrency);
  }

  activeDistributionBands() {
    if (!this.distributionReport) {
      return [];
    }
    if (this.selectedDistributionCountry === 'ALL') {
      return this.distributionReport.organizationBands;
    }
    const found = this.distributionReport.countryDistributions
      .find(item => item.countryCode === this.selectedDistributionCountry);
    return found ? found.bands : this.distributionReport.organizationBands;
  }

  payrollShare(part: number, total: number): number {
    if (!total || total <= 0) {
      return 0;
    }
    return Math.min(100, Math.round((part / total) * 100));
  }

  countryName(code: string): string {
    return this.countryNames[code] ?? code;
  }
}
