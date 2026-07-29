export interface Party {
  id: number;
  name: string;
  pixKey: string | null;
}

export interface PartyRequest {
  name: string;
  pixKey: string | null;
}
