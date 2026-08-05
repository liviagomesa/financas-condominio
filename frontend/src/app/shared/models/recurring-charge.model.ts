import { AccountType } from './account.model';
import { FundSummary } from './fund.model';
import { Party } from './party.model';

export interface RecurringCharge {
  id: number;
  type: AccountType;
  amount: number;
  dueDay: number;
  description: string;
  fund: FundSummary;
  party: Party;
  observations: string | null;
  lastGenerationFailed: boolean;
}

export interface RecurringChargeRequest {
  type: AccountType;
  amount: number;
  dueDay: number;
  description: string;
  fundId: number;
  partyId: number;
  observations: string | null;
}

export interface RecurringChargeBulkRequest {
  type: AccountType;
  amount: number;
  dueDay: number;
  description: string;
  fundId: number;
  groupId: number;
  observations: string | null;
}
