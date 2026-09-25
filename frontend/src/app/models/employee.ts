export type EmploymentStatus = 'ACTIVE' | 'INACTIVE';

export interface Employee {
  id: number;
  employeeNumber: string;
  firstName: string;
  lastName: string;
  countryCode: string;
  department: string;
  jobTitle: string;
  jobLevel: string | null;
  dateOfJoining: string;
  status: EmploymentStatus;
}

export interface EmployeePage {
  content: Employee[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface EmployeeSearch {
  query: string;
  countryCode: string;
  department: string;
  status: EmploymentStatus | '';
  page: number;
  size: number;
  sortBy: string;
  direction: 'ASC' | 'DESC';
}

import { SalaryRecord, SalaryRecordRequest } from '../features/salary-records/salary-record';

export interface EmployeeProfile {
  employee: Employee;
  currentSalary: SalaryRecord | null;
  salaryHistory: SalaryRecord[];
}

export interface CreateEmployeeRequest {
  employeeNumber: string;
  firstName: string;
  lastName: string;
  countryCode: string;
  department: string;
  jobTitle: string;
  jobLevel: string | null;
  dateOfJoining: string;
  status: EmploymentStatus;
  initialSalary: SalaryRecordRequest;
}

