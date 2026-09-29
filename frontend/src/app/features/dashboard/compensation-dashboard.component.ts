import { CommonModule } from '@angular/common';
import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { RouterLink } from '@angular/router';
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
  imports: [CommonModule, FormsModule, MatProgressBarModule, RouterLink],
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

      <mat-progress-bar *ngIf="isAnyLoading()" mode="indeterminate" class="dashboard-progress-bar"></mat-progress-bar>

      <!-- Currency Disclosure Banner -->
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

      <!-- Skeleton Disclosure Banner when initial loading -->
      <section class="disclosure-banner skeleton-banner" *ngIf="loadingCountries && !countryReport" aria-hidden="true">
        <div>
          <div class="skeleton-shimmer" style="height:14px; width:260px; margin-bottom:6px;"></div>
          <div class="skeleton-shimmer" style="height:11px; width:180px;"></div>
        </div>
        <div class="rate-chips">
          <div class="skeleton-shimmer" style="height:24px; width:90px; border-radius:14px;"></div>
          <div class="skeleton-shimmer" style="height:24px; width:90px; border-radius:14px;"></div>
        </div>
      </section>

      <!-- Global Error Banner if all calls fail -->
      <div *ngIf="hasAllErrors()" class="state-message error" role="alert">
        <span>Could not connect to the backend server. Please verify the service is running.</span>
        <button type="button" (click)="loadAllReports()">Retry all</button>
      </div>

      <!-- Section 1: KPI Summary Grid (Driven by countryReport) -->
      <section class="kpi-grid" *ngIf="countryReport as cReport" aria-label="Organization summary">
        <article class="kpi-card">
          <div class="kpi-header">
            <span>INCLUDED HEADCOUNT</span>
            <span *ngIf="loadingCountries" class="inline-spinner" title="Updating..."></span>
          </div>
          <strong>{{ cReport.totalHeadcount | number }}</strong>
          <small>{{ includeInactive ? 'Active + inactive employees' : 'Active employees as of ' + cReport.asOfDate }}</small>
        </article>
        <article class="kpi-card">
          <div class="kpi-header">
            <span>TOTAL ANNUAL PAYROLL ({{ cReport.reportingCurrency }})</span>
            <span *ngIf="loadingCountries" class="inline-spinner" title="Updating..."></span>
          </div>
          <strong>\${{ cReport.totalPayrollReportingCurrency | number:'1.2-2' }}</strong>
          <small>Normalized across {{ cReport.countries.length }} countries</small>
        </article>
        <article class="kpi-card">
          <div class="kpi-header">
            <span>ORG AVERAGE SALARY ({{ cReport.reportingCurrency }})</span>
            <span *ngIf="loadingCountries" class="inline-spinner" title="Updating..."></span>
          </div>
          <strong>\${{ cReport.overallAverageReportingCurrency | number:'1.2-2' }}</strong>
          <small>Mean annual compensation</small>
        </article>
        <article class="kpi-card">
          <div class="kpi-header">
            <span>ORG MEDIAN SALARY ({{ cReport.reportingCurrency }})</span>
            <span *ngIf="loadingCountries" class="inline-spinner" title="Updating..."></span>
          </div>
          <strong>\${{ cReport.overallMedianReportingCurrency | number:'1.2-2' }}</strong>
          <small>50th percentile annual pay</small>
        </article>
      </section>

      <!-- KPI Skeletons when initial country report is loading -->
      <section class="kpi-grid" *ngIf="loadingCountries && !countryReport" aria-label="Loading organization summary">
        <article class="kpi-card skeleton-card" *ngFor="let i of [1,2,3,4]">
          <div class="skeleton-shimmer" style="height:11px; width:65%; margin-bottom:12px;"></div>
          <div class="skeleton-shimmer" style="height:32px; width:80%; margin-bottom:8px;"></div>
          <div class="skeleton-shimmer" style="height:11px; width:55%;"></div>
        </article>
      </section>

      <!-- Section 2: Two-Column Grid (Country & Department reports load independently) -->
      <div class="two-col-grid">
        <!-- Panel A: Country Salary Report -->
        <section class="panel" aria-label="Country salary report">
          <div class="section-heading">
            <div>
              <div class="heading-with-status">
                <h2>Pay &amp; payroll by country</h2>
                <span *ngIf="loadingCountries" class="status-pill">
                  <span class="inline-spinner"></span> Loading
                </span>
              </div>
              <p>Shows local currency metrics and normalized {{ countryReport?.reportingCurrency || 'USD' }} equivalents</p>
            </div>
          </div>

          <div *ngIf="errorCountries && !countryReport" class="inline-error-banner" role="alert">
            <span>{{ errorCountries }}</span>
            <button type="button" (click)="loadCountries()">Retry</button>
          </div>

          <!-- Skeleton Table -->
          <div class="skeleton-table" *ngIf="loadingCountries && !countryReport">
            <div class="skeleton-row" *ngFor="let r of [1,2,3,4,5]">
              <div class="skeleton-shimmer" style="height:14px; width:28%;"></div>
              <div class="skeleton-shimmer" style="height:14px; width:15%;"></div>
              <div class="skeleton-shimmer" style="height:14px; width:22%;"></div>
              <div class="skeleton-shimmer" style="height:14px; width:22%;"></div>
            </div>
          </div>

          <!-- Real Country Table -->
          <div class="table-scroll" *ngIf="countryReport as cReport">
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

        <!-- Panel B: Department Salary Report (Loads as soon as available, independent of country) -->
        <section class="panel" aria-label="Department salary report">
          <div class="section-heading">
            <div>
              <div class="heading-with-status">
                <h2>Pay &amp; payroll by department</h2>
                <span *ngIf="loadingDepartments" class="status-pill">
                  <span class="inline-spinner"></span> Loading
                </span>
              </div>
              <p>Multi-country teams normalized to {{ departmentReport?.reportingCurrency || 'USD' }}</p>
            </div>
          </div>

          <div *ngIf="errorDepartments && !departmentReport" class="inline-error-banner" role="alert">
            <span>{{ errorDepartments }}</span>
            <button type="button" (click)="loadDepartments()">Retry</button>
          </div>

          <!-- Skeleton Table -->
          <div class="skeleton-table" *ngIf="loadingDepartments && !departmentReport">
            <div class="skeleton-row" *ngFor="let r of [1,2,3,4,5]">
              <div class="skeleton-shimmer" style="height:14px; width:30%;"></div>
              <div class="skeleton-shimmer" style="height:14px; width:15%;"></div>
              <div class="skeleton-shimmer" style="height:14px; width:24%;"></div>
              <div class="skeleton-shimmer" style="height:14px; width:24%;"></div>
            </div>
          </div>

          <!-- Real Department Table -->
          <div class="table-scroll" *ngIf="departmentReport as dReport">
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

      <!-- Section 3: Salary Distribution Report (Loads independently) -->
      <section class="panel" aria-label="Salary band distribution">
        <div class="section-heading">
          <div>
            <div class="heading-with-status">
              <h2>Salary distribution by band ({{ distributionReport?.reportingCurrency || 'USD' }})</h2>
              <span *ngIf="loadingDistribution" class="status-pill">
                <span class="inline-spinner"></span> Loading
              </span>
            </div>
            <p>Organization-wide and per-country distribution across \${{ (distributionReport?.bandSize || bandSize) | number:'1.0-0' }} bands</p>
          </div>
          <label class="inline-filter" *ngIf="distributionReport as dist">
            <span>Inspect country:</span>
            <select [(ngModel)]="selectedDistributionCountry">
              <option value="ALL">Organization-wide ({{ dist.totalHeadcount | number }} employees)</option>
              <option *ngFor="let c of dist.countryDistributions" [value]="c.countryCode">
                {{ countryName(c.countryCode) }} ({{ c.totalHeadcount | number }} employees)
              </option>
            </select>
          </label>
        </div>

        <div *ngIf="errorDistribution && !distributionReport" class="inline-error-banner" role="alert">
          <span>{{ errorDistribution }}</span>
          <button type="button" (click)="loadDistribution()">Retry</button>
        </div>

        <!-- Skeleton Histogram -->
        <div class="skeleton-histogram" *ngIf="loadingDistribution && !distributionReport">
          <div class="skeleton-hist-row" *ngFor="let b of [1,2,3,4,5,6]">
            <div class="skeleton-shimmer" style="height:14px; width:130px;"></div>
            <div class="skeleton-shimmer" style="height:16px; width:100%; border-radius:8px;"></div>
            <div class="skeleton-shimmer" style="height:14px; width:70px;"></div>
          </div>
        </div>

        <!-- Real Distribution -->
        <ng-container *ngIf="distributionReport as dist">
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
        </ng-container>
      </section>

      <!-- Section 4: Department Extremes Report (Loads independently) -->
      <section class="panel" aria-label="Highest and lowest paid employees by department">
        <div class="section-heading">
          <div>
            <div class="heading-with-status">
              <h2>Highest- and lowest-paid employees by department</h2>
              <span *ngIf="loadingExtremes" class="status-pill">
                <span class="inline-spinner"></span> Loading
              </span>
            </div>
            <p>Ranked by normalized {{ extremesReport?.reportingCurrency || 'USD' }} annual compensation as of {{ asOfDate }}</p>
          </div>
        </div>

        <div *ngIf="errorExtremes && !extremesReport" class="inline-error-banner" role="alert">
          <span>{{ errorExtremes }}</span>
          <button type="button" (click)="loadExtremes()">Retry</button>
        </div>

        <!-- Skeleton Extremes Grid -->
        <div class="extremes-grid" *ngIf="loadingExtremes && !extremesReport">
          <article class="extreme-card skeleton-card" *ngFor="let e of [1,2,3]">
            <div class="skeleton-shimmer" style="height:16px; width:150px; margin-bottom:14px;"></div>
            <div class="outlier-columns">
              <div class="outlier-box">
                <div class="skeleton-shimmer" style="height:13px; width:60%; margin-bottom:8px;"></div>
                <div class="skeleton-shimmer" style="height:18px; width:80%; margin-bottom:6px;"></div>
                <div class="skeleton-shimmer" style="height:12px; width:50%;"></div>
              </div>
              <div class="outlier-box">
                <div class="skeleton-shimmer" style="height:13px; width:60%; margin-bottom:8px;"></div>
                <div class="skeleton-shimmer" style="height:18px; width:80%; margin-bottom:6px;"></div>
                <div class="skeleton-shimmer" style="height:12px; width:50%;"></div>
              </div>
            </div>
          </article>
        </div>

        <!-- Real Extremes Grid -->
        <div class="extremes-grid" *ngIf="extremesReport as ext">
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
    :host {
      display: block;
      min-height: 100vh;
      background: #f4f7f5;
      color: #203a32;
      font: 14px Arial, sans-serif;
      position: relative;
    }

    /* Loading progress bar */
    .dashboard-progress-bar {
      margin-bottom: 22px;
      border-radius: 4px;
      overflow: hidden;
    }

    .page-shell { max-width: 1440px; margin: 0 auto; padding: 38px 5vw 54px; }
    .page-heading { display: flex; flex-wrap: wrap; align-items: end; justify-content: space-between; gap: 18px; margin-bottom: 20px; }
    .eyebrow { margin: 0 0 8px; color: #68877b; font-size: 10px; font-weight: 700; letter-spacing: .14em; }
    h1 { margin: 0; font-size: clamp(26px, 3vw, 34px); letter-spacing: -.04em; }
    h2 { margin: 0; font-size: 16px; }
    h3 { margin: 0; font-size: 14px; color: #294239; }
    .subtitle { margin: 8px 0 0; color: #71837b; font-size: 13px; }

    .controls-card {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 14px;
      padding: 14px 16px;
      border: 1px solid #dce6df;
      border-radius: 12px;
      background: #fff;
      box-shadow: 0 4px 16px rgba(24, 57, 42, 0.04);
    }
    .controls-card label { display: grid; gap: 5px; color: #68877b; font-size: 11px; font-weight: 600; }
    .controls-card input[type="date"], .controls-card select, .inline-filter select {
      height: 34px; padding: 0 10px; border: 1px solid #d2ddd6; border-radius: 8px; font-size: 12px; color: #203a32; background: #fff;
    }
    .checkbox-label { display: flex !important; align-items: center; gap: 8px !important; height: 34px; cursor: pointer; }

    .disclosure-banner {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      justify-content: space-between;
      gap: 14px;
      margin-bottom: 20px;
      padding: 14px 18px;
      border: 1px solid #cce0d3;
      border-radius: 12px;
      background: #ebf4ee;
    }
    .disclosure-banner strong { font-size: 12px; color: #1d4736; }
    .disclosure-banner p { margin: 4px 0 0; color: #49695b; font-size: 11px; }
    .rate-chips { display: flex; flex-wrap: wrap; gap: 8px; }
    .rate-chip { padding: 4px 9px; border-radius: 14px; background: #fff; border: 1px solid #cde0d4; color: #2a5442; font-size: 11px; font-weight: 600; }

    /* KPI Cards */
    .kpi-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; margin-bottom: 18px; }
    .kpi-card {
      padding: 18px 20px;
      border: 1px solid #e0e8e2;
      border-radius: 12px;
      background: #fff;
      box-shadow: 0 8px 24px rgba(24, 57, 42, 0.04);
      position: relative;
    }
    .kpi-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
    }
    .kpi-card span { color: #7a8e84; font-size: 10px; font-weight: 700; letter-spacing: .08em; }
    .kpi-card strong { display: block; margin: 8px 0 4px; color: #1d3b31; font-size: 24px; }
    .kpi-card small { color: #7c8e85; font-size: 11px; }

    .two-col-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 18px; }
    .panel {
      margin-bottom: 18px;
      border: 1px solid #e0e8e2;
      border-radius: 12px;
      background: #fff;
      box-shadow: 0 8px 24px rgba(24, 57, 42, 0.04);
      overflow: hidden;
    }
    .section-heading { display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: 14px; padding: 18px 20px 12px; }
    .section-heading p { margin: 4px 0 0; color: #7b8d84; font-size: 11px; }
    .heading-with-status { display: flex; align-items: center; gap: 10px; }
    .status-pill {
      display: inline-flex;
      align-items: center;
      gap: 5px;
      padding: 2px 8px;
      border-radius: 12px;
      font-size: 10px;
      font-weight: 700;
      letter-spacing: .04em;
      background: #eef7f2;
      color: #2b6146;
      border: 1px solid #cfe5d7;
    }

    .inline-spinner {
      display: inline-block;
      width: 11px;
      height: 11px;
      border: 2px solid #a8c4b4;
      border-top-color: #10b981;
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
    }
    @keyframes spin {
      0% { transform: rotate(0deg); }
      100% { transform: rotate(360deg); }
    }

    .inline-filter { display: flex; align-items: center; gap: 8px; font-size: 12px; color: #5a6e64; font-weight: 600; }
    .table-scroll { overflow-x: auto; }
    table { width: 100%; border-collapse: collapse; }
    th, td { padding: 12px 16px; border-bottom: 1px solid #edf1ee; text-align: left; font-size: 12px; }
    th { color: #7e9188; font-size: 10px; font-weight: 700; letter-spacing: .08em; background: #fafcfb; }
    .payroll-cell { display: grid; gap: 5px; }
    .mini-bar { height: 5px; width: 110px; border-radius: 4px; background: #ebf0ed; overflow: hidden; }
    .mini-bar span { display: block; height: 100%; background: #3c7156; }

    /* Histogram */
    .histogram-list { display: grid; gap: 10px; padding: 8px 20px 18px; }
    .histogram-row { display: grid; grid-template-columns: 150px 1fr 110px; align-items: center; gap: 14px; }
    .band-label { font-size: 12px; font-weight: 600; color: #314b40; }
    .bar-track { height: 16px; border-radius: 8px; background: #edf2ef; overflow: hidden; }
    .bar-fill { height: 100%; border-radius: 8px; background: linear-gradient(90deg, #265c47, #5ca37d); transition: width 0.4s ease; }
    .band-stats { text-align: right; font-size: 12px; }
    .band-stats span { margin-left: 5px; color: #7c8f85; font-size: 11px; }
    .country-matrix-wrap { padding: 8px 20px 18px; border-top: 1px solid #edf1ee; }
    .country-matrix-wrap h3 { margin: 8px 0 12px; }
    .pct-note { display: inline; margin-left: 4px; color: #82948b; font-size: 10px; }

    /* Extremes */
    .extremes-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(360px, 1fr)); gap: 14px; padding: 8px 20px 20px; }
    .extreme-card { padding: 16px; border: 1px solid #e3ebe5; border-radius: 10px; background: #fafcfb; }
    .extreme-card header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
    .extreme-card header span { color: #7b8e85; font-size: 11px; }
    .outlier-columns { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
    .outlier-box { padding: 12px; border-radius: 8px; background: #fff; border: 1px solid #e6eee8; }
    .badge { display: inline-block; margin-bottom: 6px; padding: 3px 7px; border-radius: 10px; font-size: 9px; font-weight: 700; letter-spacing: .06em; }
    .high-badge { background: #e6f3ea; color: #2e6b48; }
    .low-badge { background: #f3efe6; color: #7b5c29; }
    .emp-link { display: block; color: #1d4234; font-size: 12px; font-weight: 700; text-decoration: none; }
    .emp-link:hover { text-decoration: underline; }
    .role-meta { margin: 4px 0 8px; color: #778a80; font-size: 11px; }
    .pay-highlight { margin: 0 0 2px; color: #203a32; font-size: 15px; font-weight: 700; }
    .outlier-box small { color: #7e9087; font-size: 10px; }

    /* Skeleton Shimmer Loading States */
    .skeleton-shimmer {
      display: block;
      background: linear-gradient(90deg, #edf3ef 25%, #e1ebe5 50%, #edf3ef 75%);
      background-size: 200% 100%;
      animation: shimmer 1.5s infinite ease-in-out;
      border-radius: 6px;
    }
    @keyframes shimmer {
      0% { background-position: -200% 0; }
      100% { background-position: 200% 0; }
    }
    .skeleton-card { min-height: 110px; }
    .skeleton-banner { min-height: 60px; }
    .skeleton-table { padding: 12px 20px 20px; display: grid; gap: 14px; }
    .skeleton-row { display: flex; align-items: center; justify-content: space-between; gap: 16px; padding: 8px 0; border-bottom: 1px solid #edf1ee; }
    .skeleton-histogram { padding: 12px 20px 22px; display: grid; gap: 12px; }
    .skeleton-hist-row { display: grid; grid-template-columns: 130px 1fr 70px; align-items: center; gap: 14px; }

    .state-message { padding: 20px; border-radius: 10px; background: #fff; margin-bottom: 18px; }
    .state-message.error { display: flex; align-items: center; justify-content: space-between; color: #a23434; border: 1px solid #fad2d2; background: #fff5f5; }
    .state-message button, .inline-error-banner button {
      padding: 6px 12px; border-radius: 6px; border: 1px solid #c97979; background: #fff; color: #a23434; font-weight: 600; cursor: pointer;
    }
    .inline-error-banner {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin: 12px 20px;
      padding: 10px 14px;
      border-radius: 8px;
      background: #fff5f5;
      border: 1px solid #fad2d2;
      color: #991b1b;
      font-size: 12px;
    }

    @media(max-width: 1050px) {
      .kpi-grid { grid-template-columns: 1fr 1fr; }
      .two-col-grid { grid-template-columns: 1fr; }
    }
    @media(max-width: 640px) {
      .kpi-grid { grid-template-columns: 1fr; }
      .histogram-row, .skeleton-hist-row { grid-template-columns: 110px 1fr 65px; }
      .outlier-columns { grid-template-columns: 1fr; }
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

  loadingCountries = false;
  loadingDepartments = false;
  loadingDistribution = false;
  loadingExtremes = false;

  errorCountries = '';
  errorDepartments = '';
  errorDistribution = '';
  errorExtremes = '';

  ngOnInit(): void {
    this.loadAllReports();
  }

  loadAllReports(): void {
    this.loadCountries();
    this.loadDepartments();
    this.loadDistribution();
    this.loadExtremes();
  }

  loadCountries(): void {
    this.loadingCountries = true;
    this.errorCountries = '';
    this.reportApi.countryReport(this.asOfDate, this.includeInactive)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          this.countryReport = res;
          this.loadingCountries = false;
        },
        error: () => {
          this.loadingCountries = false;
          this.errorCountries = 'Could not load country payroll report.';
        }
      });
  }

  loadDepartments(): void {
    this.loadingDepartments = true;
    this.errorDepartments = '';
    this.reportApi.departmentReport(this.asOfDate, this.includeInactive)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          this.departmentReport = res;
          this.loadingDepartments = false;
        },
        error: () => {
          this.loadingDepartments = false;
          this.errorDepartments = 'Could not load department payroll report.';
        }
      });
  }

  loadDistribution(): void {
    this.loadingDistribution = true;
    this.errorDistribution = '';
    this.reportApi.distributionReport(this.asOfDate, this.includeInactive, this.bandSize)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          this.distributionReport = res;
          this.loadingDistribution = false;
        },
        error: () => {
          this.loadingDistribution = false;
          this.errorDistribution = 'Could not load salary band distribution.';
        }
      });
  }

  loadExtremes(): void {
    this.loadingExtremes = true;
    this.errorExtremes = '';
    this.reportApi.departmentExtremesReport(this.asOfDate, this.includeInactive, 3)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          this.extremesReport = res;
          this.loadingExtremes = false;
        },
        error: () => {
          this.loadingExtremes = false;
          this.errorExtremes = 'Could not load department pay extremes.';
        }
      });
  }

  loadDistributionOnly(): void {
    this.loadDistribution();
  }

  isAnyLoading(): boolean {
    return this.loadingCountries || this.loadingDepartments || this.loadingDistribution || this.loadingExtremes;
  }

  hasAllErrors(): boolean {
    return !!(this.errorCountries && this.errorDepartments && this.errorDistribution && this.errorExtremes)
      && !this.countryReport && !this.departmentReport && !this.distributionReport && !this.extremesReport;
  }

  hasAnyError(): boolean {
    return !!(this.errorCountries || this.errorDepartments || this.errorDistribution || this.errorExtremes);
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
