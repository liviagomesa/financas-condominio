import { Unit } from './unit.model';

export interface Supplier {
  id: number;
  name: string;
  unit: Unit | null;
  pixKey: string | null;
}

export interface SupplierRequest {
  name: string;
  unitId: number | null;
  pixKey: string | null;
}
