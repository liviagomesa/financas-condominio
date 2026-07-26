import { Unit } from './unit.model';

export interface Resident {
  id: number;
  name: string;
  unit: Unit;
  email: string | null;
  phone: string | null;
}

export interface ResidentRequest {
  name: string;
  unitId: number;
  email: string | null;
  phone: string | null;
}
