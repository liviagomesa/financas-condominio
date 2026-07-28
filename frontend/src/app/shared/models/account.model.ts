import { Supplier } from './supplier.model';
import { Unit } from './unit.model';

export type AccountType = 'RECEIVABLE' | 'PAYABLE';

export const ACCOUNT_TYPE_LABELS: Record<AccountType, string> = {
  RECEIVABLE: 'Entrada',
  PAYABLE: 'Saída',
};

export type Fund = 'POOL' | 'POOL_GARDEN' | 'SIDE_GARDEN';

export const FUND_LABELS: Record<Fund, string> = {
  POOL: 'Piscina',
  POOL_GARDEN: 'Jardim Piscina',
  SIDE_GARDEN: 'Jardim Lateral',
};

export interface Account {
  id: number;
  type: AccountType;
  amount: number;
  dueDate: string;
  description: string;
  fund: Fund;
  recurring: boolean;
  paymentDate: string | null;
  observations: string | null;
  unit: Unit | null;
  supplier: Supplier | null;
}

export interface AccountRequest {
  type: AccountType;
  amount: number;
  dueDate: string;
  description: string;
  fund: Fund;
  recurring: boolean;
  unitId: number | null;
  supplierId: number | null;
  paymentDate: string | null;
  observations: string | null;
}

export interface AccountBulkRequest {
  amount: number;
  dueDate: string;
  description: string;
  fund: Fund;
  recurring: boolean;
  paymentDate: string | null;
  observations: string | null;
}

export interface AccountPaymentRequest {
  paymentDate: string;
}

export interface AccountFilters {
  unitId?: number;
  supplierId?: number;
  type?: AccountType;
  paid?: boolean;
  overdue?: boolean;
  dueYearMonth?: string;
  paymentYearMonth?: string;
}
