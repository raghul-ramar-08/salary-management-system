export interface CountrySalaryMetrics {
  countryCode: string;
  headcount: number;
  localCurrencyCode: string;
  averageSalaryLocal: number | null;
  medianSalaryLocal: number | null;
  totalPayrollLocal: number | null;
  averageSalaryReportingCurrency: number;
  medianSalaryReportingCurrency: number;
  totalPayrollReportingCurrency: number;
}

export interface CountrySalaryReportResponse {
  asOfDate: string;
  includeInactive: boolean;
  reportingCurrency: string;
  rateBasis: string;
  exchangeRatesToReportingCurrency: Record<string, number>;
  totalHeadcount: number;
  totalPayrollReportingCurrency: number;
  overallAverageReportingCurrency: number;
  overallMedianReportingCurrency: number;
  countries: CountrySalaryMetrics[];
}

export interface DepartmentSalaryMetrics {
  department: string;
  headcount: number;
  averageSalaryReportingCurrency: number;
  medianSalaryReportingCurrency: number;
  totalPayrollReportingCurrency: number;
}

export interface DepartmentSalaryReportResponse {
  asOfDate: string;
  includeInactive: boolean;
  reportingCurrency: string;
  rateBasis: string;
  exchangeRatesToReportingCurrency: Record<string, number>;
  totalHeadcount: number;
  totalPayrollReportingCurrency: number;
  departments: DepartmentSalaryMetrics[];
}

export interface SalaryBandBucket {
  label: string;
  minInclusive: number;
  maxExclusive: number | null;
  headcount: number;
  percentageOfTotal: number;
}

export interface CountryBandDistribution {
  countryCode: string;
  totalHeadcount: number;
  bands: SalaryBandBucket[];
}

export interface SalaryDistributionReportResponse {
  asOfDate: string;
  includeInactive: boolean;
  reportingCurrency: string;
  rateBasis: string;
  countryCodeFilter: string | null;
  bandSize: number;
  totalHeadcount: number;
  organizationBands: SalaryBandBucket[];
  countryDistributions: CountryBandDistribution[];
}

export interface EmployeeCompensationSnapshot {
  employeeId: number;
  employeeNumber: string;
  fullName: string;
  countryCode: string;
  jobTitle: string;
  jobLevel: string | null;
  localAmount: number;
  localCurrencyCode: string;
  reportingCurrencyAmount: number;
  effectiveDate: string;
}

export interface DepartmentExtremes {
  department: string;
  headcount: number;
  highestPaid: EmployeeCompensationSnapshot | null;
  lowestPaid: EmployeeCompensationSnapshot | null;
  highestPaidEmployees: EmployeeCompensationSnapshot[];
  lowestPaidEmployees: EmployeeCompensationSnapshot[];
}

export interface DepartmentSalaryExtremesReportResponse {
  asOfDate: string;
  includeInactive: boolean;
  reportingCurrency: string;
  rateBasis: string;
  limitPerSide: number;
  departments: DepartmentExtremes[];
}
