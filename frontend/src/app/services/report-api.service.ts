import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  CountrySalaryReportResponse,
  DepartmentSalaryExtremesReportResponse,
  DepartmentSalaryReportResponse,
  SalaryDistributionReportResponse
} from '../models/reports';

@Injectable({ providedIn: 'root' })
export class ReportApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/reports';

  countryReport(asOfDate: string, includeInactive: boolean): Observable<CountrySalaryReportResponse> {
    const params = this.baseParams(asOfDate, includeInactive);
    return this.http.get<CountrySalaryReportResponse>(`${this.baseUrl}/countries`, { params });
  }

  departmentReport(asOfDate: string, includeInactive: boolean): Observable<DepartmentSalaryReportResponse> {
    const params = this.baseParams(asOfDate, includeInactive);
    return this.http.get<DepartmentSalaryReportResponse>(`${this.baseUrl}/departments`, { params });
  }

  distributionReport(
    asOfDate: string,
    includeInactive: boolean,
    bandSize: number,
    countryCode?: string
  ): Observable<SalaryDistributionReportResponse> {
    let params = this.baseParams(asOfDate, includeInactive).set('bandSize', bandSize);
    if (countryCode) {
      params = params.set('countryCode', countryCode);
    }
    return this.http.get<SalaryDistributionReportResponse>(`${this.baseUrl}/distribution`, { params });
  }

  departmentExtremesReport(
    asOfDate: string,
    includeInactive: boolean,
    limit = 3
  ): Observable<DepartmentSalaryExtremesReportResponse> {
    const params = this.baseParams(asOfDate, includeInactive).set('limit', limit);
    return this.http.get<DepartmentSalaryExtremesReportResponse>(`${this.baseUrl}/department-extremes`, { params });
  }

  private baseParams(asOfDate: string, includeInactive: boolean): HttpParams {
    let params = new HttpParams().set('includeInactive', includeInactive);
    if (asOfDate) {
      params = params.set('asOfDate', asOfDate);
    }
    return params;
  }
}
