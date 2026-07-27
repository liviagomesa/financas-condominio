import { Observable, forkJoin, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { ApiError } from '../core/error.interceptor';

export interface BulkDeleteResult {
  succeeded: number[];
  failed: { id: number; message: string }[];
}

/**
 * Remove cada id individualmente (melhor esforço), reaproveitando o DELETE /{id} já
 * existente de cada recurso — sem endpoint transacional novo.
 */
export function bulkDelete(
  ids: number[],
  deleteFn: (id: number) => Observable<void>
): Observable<BulkDeleteResult> {
  if (ids.length === 0) {
    return of({ succeeded: [], failed: [] });
  }

  const attempts = ids.map((id) =>
    deleteFn(id).pipe(
      map(() => ({ id, ok: true as const, message: '' })),
      catchError((error: ApiError) =>
        of({ id, ok: false as const, message: error?.message ?? 'Falha ao remover.' })
      )
    )
  );

  return forkJoin(attempts).pipe(
    map((results) => ({
      succeeded: results.filter((r) => r.ok).map((r) => r.id),
      failed: results.filter((r) => !r.ok).map((r) => ({ id: r.id, message: r.message })),
    }))
  );
}
