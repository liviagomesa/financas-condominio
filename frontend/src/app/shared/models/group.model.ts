import { Party } from './party.model';

export interface Group {
  id: number;
  name: string;
  members: Party[];
}

export interface GroupRequest {
  name: string;
  partyIds: number[];
}
