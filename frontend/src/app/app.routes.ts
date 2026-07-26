import { Routes } from '@angular/router';
import { UnitList } from './unit/unit-list/unit-list';
import { UnitForm } from './unit/unit-form/unit-form';
import { ResidentList } from './resident/resident-list/resident-list';
import { ResidentForm } from './resident/resident-form/resident-form';
import { ReceivableList } from './receivable/receivable-list/receivable-list';
import { ReceivableForm } from './receivable/receivable-form/receivable-form';

export const routes: Routes = [
  { path: '', redirectTo: 'units', pathMatch: 'full' },
  { path: 'units', component: UnitList },
  { path: 'units/new', component: UnitForm },
  { path: 'units/:id/edit', component: UnitForm },
  { path: 'residents', component: ResidentList },
  { path: 'residents/new', component: ResidentForm },
  { path: 'residents/:id/edit', component: ResidentForm },
  { path: 'receivables', component: ReceivableList },
  { path: 'receivables/new', component: ReceivableForm },
  { path: 'receivables/:id/edit', component: ReceivableForm },
];
