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

  // para cada id, chama a função de delete e converte o retorno em outro Observable que nunca emite erro
  const attempts = ids.map((id) =>
    deleteFn(id).pipe(
      // em caso de sucesso, mapeia para { id, ok: true, message: '' }
      map(() => ({ id, ok: true as const, message: '' })),
      // em caso de erro, converte o erro em { id, ok: false, message: ... }
      catchError((error: ApiError) =>
        // Fallback nunca será executado, pois o error.interceptor garante que message exista
        of({ id, ok: false as const, message: error?.message ?? 'Falha ao remover.' })
      )
    )
  );

  // passamos para o forkJoin um Observable[] → espera todas as chamadas terminarem
  return forkJoin(attempts).pipe(
    map((results) => ({
      succeeded: results.filter((r) => r.ok).map((r) => r.id),
      failed: results.filter((r) => !r.ok).map((r) => ({ id: r.id, message: r.message })),
    }))
  );
}
