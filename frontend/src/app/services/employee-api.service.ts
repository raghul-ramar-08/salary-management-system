import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { EmployeePage, EmployeeSearch } from '../models/employee';

@Injectable({ providedIn: 'root' })
export class EmployeeApiService {
  private readonly http = inject(HttpClient);
  private readonly endpoint = '/api/employees';

  search(criteria: EmployeeSearch): Observable<EmployeePage> {
    let params = new HttpParams()
      .set('page', criteria.page)
      .set('size', criteria.size)
      .set('sortBy', criteria.sortBy)
      .set('direction', criteria.direction);

    if (criteria.query.trim()) {
      params = params.set('q', criteria.query.trim());
    }
    if (criteria.countryCode) {
      params = params.set('countryCode', criteria.countryCode);
    }
    if (criteria.department) {
      params = params.set('department', criteria.department);
    }
    if (criteria.status) {
      params = params.set('status', criteria.status);
    }

    console.debug('[employee-directory] GET employees', {
      page: criteria.page, size: criteria.size,
      filtersApplied: Boolean(criteria.query.trim() || criteria.countryCode || criteria.department || criteria.status)
    });
    return this.http.get<EmployeePage>(this.endpoint, { params });
  }
}
