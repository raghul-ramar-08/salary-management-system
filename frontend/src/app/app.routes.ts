import { Routes } from '@angular/router';
import { EmployeeDirectoryComponent } from './employee-directory.component';
import { EmployeeProfileComponent } from './employee-profile.component';
import { SalaryRecordsComponent } from './features/salary-records/salary-records.component';

export const appRoutes: Routes = [
  { path: '', component: EmployeeDirectoryComponent },
  { path: 'employees/:id', component: EmployeeProfileComponent },
  { path: 'salary-records', component: SalaryRecordsComponent },
  { path: '**', redirectTo: '' }
];
