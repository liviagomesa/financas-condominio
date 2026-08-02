import { Fund } from './fund.model';
import { Party } from './party.model';

export type AccountType = 'RECEIVABLE' | 'PAYABLE';

export const ACCOUNT_TYPE_LABELS: Record<AccountType, string> = {
  RECEIVABLE: 'Entrada',
  PAYABLE: 'Saída',
};

export interface Account {
  id: number;
  type: AccountType;
  amount: number;
  dueDate: string;
  description: string;
  fund: Fund;
  paymentDate: string | null;
  observations: string | null;
  party: Party;
}

export interface AccountRequest {
  type: AccountType;
  amount: number;
  dueDate: string;
  description: string;
  fundId: number;
  partyId: number;
  paymentDate: string | null;
  observations: string | null;
}

export interface AccountBulkRequest {
  type: AccountType;
  amount: number;
  dueDate: string;
  description: string;
  fundId: number;
  groupId: number;
  paymentDate: string | null;
  observations: string | null;
}

export interface AccountPaymentRequest {
  paymentDate: string;
  paidAmount?: number;
}

export interface AccountDuplicateRequest {
  zeroAmount: boolean;
}

export interface AccountFilters {
  partyId?: number;
  fundId?: number;
  type?: AccountType;
  paid?: boolean;
  overdue?: boolean;
  dueYearMonth?: string;
  paymentYearMonth?: string;
}
