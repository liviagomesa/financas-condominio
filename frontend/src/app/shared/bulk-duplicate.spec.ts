import { of, throwError } from 'rxjs';
import { describe, expect, it } from 'vitest';
import { bulkDuplicate } from './bulk-duplicate';
import { Account } from './models/account.model';

function account(id: number): Account {
  return {
    id,
    type: 'RECEIVABLE',
    amount: 350,
    dueDate: '2026-09-10',
    description: 'Taxa condominial',
    fund: { id: 1, name: 'Piscina', initialBalance: 0 },
    paymentDate: null,
    observations: null,
    party: { id: 1, name: 'Bloco A - 101', pixKey: null },
  };
}

describe('bulkDuplicate', () => {
  it('returns empty result and never calls duplicateFn when no ids are given', async () => {
    let called = false;
    const result = await new Promise((resolve) =>
      bulkDuplicate([], (id) => {
        called = true;
        return of(account(id));
      }).subscribe(resolve)
    );
    expect(result).toEqual({ succeeded: [], failed: [] });
    expect(called).toBe(false);
  });

  it('reports all as succeeded when every duplication works', async () => {
    const result: any = await new Promise((resolve) =>
      bulkDuplicate([1, 2, 3], (id) => of(account(id))).subscribe(resolve)
    );
    expect(result.succeeded.map((a: Account) => a.id)).toEqual([1, 2, 3]);
    expect(result.failed).toEqual([]);
  });

  it('aggregates succeeded and failed independently for mixed results without interrupting the others', async () => {
    const duplicateFn = (id: number) =>
      id === 2
        ? throwError(() => ({ status: 404, message: 'Conta não encontrada.' }))
        : of(account(id));
    const result: any = await new Promise((resolve) =>
      bulkDuplicate([1, 2, 3], duplicateFn).subscribe(resolve)
    );
    expect(result.succeeded.map((a: Account) => a.id)).toEqual([1, 3]);
    expect(result.failed).toEqual([{ id: 2, message: 'Conta não encontrada.' }]);
  });
});
