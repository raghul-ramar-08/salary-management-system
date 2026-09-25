export interface SalaryRecord {
  id: number;
  employeeNumber: string;
  employeeName: string;
  countryCode: string;
  department: string;
  amount: number;
  currencyCode: string;
  effectiveDate: string;
  effectiveTo: string | null;
  changeReason: string;
  recordedAt: string;
}

export interface SalaryRecordPage {
  content: SalaryRecord[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface SalaryRecordSearch {
  employeeNumber: string;
  countryCode: string;
  department: string;
  currencyCode: string;
  effectiveFrom: string;
  effectiveTo: string;
  currentOnly: boolean;
  page: number;
  size: number;
  sortBy: string;
  direction: 'ASC' | 'DESC';
}

export interface SalaryRecordRequest {
  amount: number;
  currencyCode: string;
  effectiveDate: string;
  changeReason: string;
}
