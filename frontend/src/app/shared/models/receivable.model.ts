import { Unit } from './unit.model';

export type TargetAccount = 'POOL' | 'POOL_GARDEN' | 'SIDE_GARDEN';

export const TARGET_ACCOUNT_LABELS: Record<TargetAccount, string> = {
  POOL: 'Piscina',
  POOL_GARDEN: 'Jardim Piscina',
  SIDE_GARDEN: 'Jardim Lateral',
};

export interface Receivable {
  id: number;
  amount: number;
  dueDate: string;
  description: string;
  targetAccount: TargetAccount;
  recurring: boolean;
  paymentDate: string | null;
  unit: Unit;
}

export interface ReceivableRequest {
  amount: number;
  dueDate: string;
  description: string;
  targetAccount: TargetAccount;
  recurring: boolean;
  unitId: number;
  paymentDate: string | null;
}

export interface ReceivableBulkRequest {
  amount: number;
  dueDate: string;
  description: string;
  targetAccount: TargetAccount;
  recurring: boolean;
  paymentDate: string | null;
}

export interface ReceivablePaymentRequest {
  paymentDate: string;
}

export interface ReceivableFilters {
  unitId?: number;
  paid?: boolean;
  overdue?: boolean;
  dueYearMonth?: string;
  paymentYearMonth?: string;
}
