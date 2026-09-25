import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { SalaryRecord, SalaryRecordPage, SalaryRecordRequest, SalaryRecordSearch } from './salary-record';

@Injectable({ providedIn: 'root' })
export class SalaryApiService {
  private readonly http = inject(HttpClient);
  private readonly endpoint = '/api/salary-records';

  search(criteria: SalaryRecordSearch): Observable<SalaryRecordPage> {
    let params = new HttpParams()
      .set('page', criteria.page)
      .set('size', criteria.size)
      .set('sortBy', criteria.sortBy)
      .set('direction', criteria.direction);

    if (criteria.currentOnly) params = params.set('currentOnly', true);

    if (criteria.employeeNumber.trim()) params = params.set('employeeNumber', criteria.employeeNumber.trim());
    if (criteria.countryCode) params = params.set('countryCode', criteria.countryCode);
    if (criteria.department) params = params.set('department', criteria.department);
    if (criteria.currencyCode) params = params.set('currencyCode', criteria.currencyCode);
    if (criteria.effectiveFrom) params = params.set('effectiveFrom', criteria.effectiveFrom);
    if (criteria.effectiveTo) params = params.set('effectiveTo', criteria.effectiveTo);

    const filtersApplied = Boolean(criteria.employeeNumber.trim() || criteria.countryCode
      || criteria.department || criteria.currencyCode || criteria.effectiveFrom || criteria.effectiveTo
      || criteria.currentOnly);
    console.debug('[salary-records] Fetching page', {
      page: criteria.page, size: criteria.size, filtersApplied
    });
    return this.http.get<SalaryRecordPage>(this.endpoint, { params }).pipe(
      tap({
        next: result => console.debug('[salary-records] Page loaded', {
          returned: result.content.length, total: result.totalElements
        }),
        error: error => console.error('[salary-records] Fetch failed', error)
      })
    );
  }

  add(employeeNumber: string, request: SalaryRecordRequest): Observable<SalaryRecord> {
    const normalizedNumber = employeeNumber.trim().toUpperCase();
    const endpoint = `/api/employees/${encodeURIComponent(normalizedNumber)}/salary-records`;
    console.debug('[salary-records] Creating record', { employeeNumber: normalizedNumber });
    return this.http.post<SalaryRecord>(endpoint, request).pipe(
      tap({
        next: () => console.debug('[salary-records] Record created'),
        error: error => console.error('[salary-records] Record creation failed', error)
      })
    );
  }
}
