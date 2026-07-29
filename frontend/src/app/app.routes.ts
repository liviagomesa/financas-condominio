import { Routes } from '@angular/router';
import { UnitList } from './unit/unit-list/unit-list';
import { UnitForm } from './unit/unit-form/unit-form';
import { SupplierList } from './supplier/supplier-list/supplier-list';
import { SupplierForm } from './supplier/supplier-form/supplier-form';
import { AccountList } from './account/account-list/account-list';
import { AccountForm } from './account/account-form/account-form';
import { FundList } from './fund/fund-list/fund-list';
import { FundForm } from './fund/fund-form/fund-form';

export const routes: Routes = [
  { path: '', redirectTo: 'units', pathMatch: 'full' },
  { path: 'units', component: UnitList },
  { path: 'units/new', component: UnitForm },
  { path: 'units/:id/edit', component: UnitForm },
  { path: 'suppliers', component: SupplierList },
  { path: 'suppliers/new', component: SupplierForm },
  { path: 'suppliers/:id/edit', component: SupplierForm },
  { path: 'accounts', component: AccountList },
  { path: 'accounts/new', component: AccountForm },
  { path: 'accounts/:id/edit', component: AccountForm },
  { path: 'funds', component: FundList },
  { path: 'funds/new', component: FundForm },
  { path: 'funds/:id/edit', component: FundForm },
];
