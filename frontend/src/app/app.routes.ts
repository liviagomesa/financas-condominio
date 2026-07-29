import { Routes } from '@angular/router';
import { PartyList } from './party/party-list/party-list';
import { PartyForm } from './party/party-form/party-form';
import { GroupList } from './group/group-list/group-list';
import { GroupForm } from './group/group-form/group-form';
import { AccountList } from './account/account-list/account-list';
import { AccountForm } from './account/account-form/account-form';
import { FundList } from './fund/fund-list/fund-list';
import { FundForm } from './fund/fund-form/fund-form';

export const routes: Routes = [
  { path: '', redirectTo: 'parties', pathMatch: 'full' },
  { path: 'parties', component: PartyList },
  { path: 'parties/new', component: PartyForm },
  { path: 'parties/:id/edit', component: PartyForm },
  { path: 'groups', component: GroupList },
  { path: 'groups/new', component: GroupForm },
  { path: 'groups/:id/edit', component: GroupForm },
  { path: 'accounts', component: AccountList },
  { path: 'accounts/new', component: AccountForm },
  { path: 'accounts/:id/edit', component: AccountForm },
  { path: 'funds', component: FundList },
  { path: 'funds/new', component: FundForm },
  { path: 'funds/:id/edit', component: FundForm },
];
