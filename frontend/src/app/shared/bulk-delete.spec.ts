import { of, throwError } from 'rxjs';
import { describe, expect, it } from 'vitest';
import { bulkDelete } from './bulk-delete';

describe('bulkDelete', () => {
  it('returns empty result when no ids are given', async () => {
    const result = await new Promise((resolve) =>
      bulkDelete([], () => of(undefined)).subscribe(resolve)
    );
    expect(result).toEqual({ succeeded: [], failed: [] });
  });

  it('reports all as succeeded when every deletion works', async () => {
    const result = await new Promise((resolve) =>
      bulkDelete([1, 2, 3], () => of(undefined)).subscribe(resolve)
    );
    expect(result).toEqual({ succeeded: [1, 2, 3], failed: [] });
  });

  it('reports all as failed when every deletion fails', async () => {
    const deleteFn = () => throwError(() => ({ status: 409, message: 'Vínculo existente.' }));
    const result: any = await new Promise((resolve) => bulkDelete([1, 2], deleteFn).subscribe(resolve));
    expect(result.succeeded).toEqual([]);
    expect(result.failed).toEqual([
      { id: 1, message: 'Vínculo existente.' },
      { id: 2, message: 'Vínculo existente.' },
    ]);
  });

  it('aggregates succeeded and failed independently for mixed results', async () => {
    const deleteFn = (id: number) =>
      id === 2 ? throwError(() => ({ status: 409, message: 'Vínculo existente.' })) : of(undefined);
    const result: any = await new Promise((resolve) =>
      bulkDelete([1, 2, 3], deleteFn).subscribe(resolve)
    );
    expect(result.succeeded).toEqual([1, 3]);
    expect(result.failed).toEqual([{ id: 2, message: 'Vínculo existente.' }]);
  });
});
