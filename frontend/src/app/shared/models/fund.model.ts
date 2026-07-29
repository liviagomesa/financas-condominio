export interface Fund {
  id: number;
  name: string;
  initialBalance: number;
  realBalance: number;
}

export interface FundRequest {
  name: string;
  initialBalance: number;
}
