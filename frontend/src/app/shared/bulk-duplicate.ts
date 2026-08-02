import { Observable, forkJoin, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { ApiError } from '../core/error.interceptor';
import { Account } from './models/account.model';

export interface BulkDuplicateResult {
  succeeded: Account[];
  failed: { id: number; message: string }[];
}

/**
 * Duplica cada id individualmente (melhor esforço), reaproveitando o POST /{id}/duplicate
 * já existente — sem endpoint transacional novo.
 */
export function bulkDuplicate(
  ids: number[],
  duplicateFn: (id: number) => Observable<Account>
): Observable<BulkDuplicateResult> {
  if (ids.length === 0) {
    return of({ succeeded: [], failed: [] });
  }

  const attempts = ids.map((id) =>
    duplicateFn(id).pipe(
      map((account) => ({ id, ok: true as const, account, message: '' })),
      catchError((error: ApiError) =>
        of({ id, ok: false as const, account: null, message: error?.message ?? 'Falha ao duplicar.' })
      )
    )
  );

  return forkJoin(attempts).pipe(
    map((results) => ({
      succeeded: results.filter((r) => r.ok).map((r) => r.account as Account),
      failed: results.filter((r) => !r.ok).map((r) => ({ id: r.id, message: r.message })),
    }))
  );
}
