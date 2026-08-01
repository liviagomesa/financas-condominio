import { Signal, computed, signal } from '@angular/core';

/** Contrato genérico para estado de seleção múltipla numa listagem. */
export interface Selection<T> {
  readonly selectedIds: Signal<ReadonlySet<number>>;
  readonly selectedCount: Signal<number>;
  isSelected(item: T): boolean;
  toggle(item: T): void;
  toggleWithRange(item: T, items: T[], shiftKey: boolean): void;
  toggleAll(items: T[]): void;
  isAllSelected(items: T[]): boolean;
  clear(): void;
}

/** Factory que cria uma instância de estado de seleção múltipla numa listagem. */
export function createSelection<T>(getId: (item: T) => number): Selection<T> {
  // Signal de objeto/Set: precisa criar com new toda vez que setar para propiciar detecção de mudança.
  // A factory sempre cria esse set vazio, não tem parâmetro tipo "initialIds" para nascer com pré-seleção
  // Pois a intenção é que o componente chame as funções, que fazem ids.set()
  //
  // É exposto no final como readonly (selectedIds) para impedir set direto (só através das funções)
  const ids = signal<ReadonlySet<number>>(new Set());
  const anchorId = signal<number | null>(null);

  /** Verifica se o ID do item está no set de IDs. */
  function isSelected(item: T): boolean {
    return ids().has(getId(item));
  }

  /** Adiciona ou remove o ID do set (clique em uma linha). */
  function toggle(item: T): void {
    const id = getId(item);
    const next = new Set(ids());
    if (next.has(id)) {
      next.delete(id);
    } else {
      next.add(id);
    }
    ids.set(next);
  }

  function toggleWithRange(item: T, items: T[], shiftKey: boolean): void {
    const anchor = anchorId();
    if (!shiftKey || anchor == null) {
      toggle(item);
      anchorId.set(getId(item));
      return;
    }

    const anchorIndex = items.findIndex((i) => getId(i) === anchor);
    const itemIndex = items.findIndex((i) => getId(i) === getId(item));
    if (anchorIndex === -1 || itemIndex === -1) {
      toggle(item);
      anchorId.set(getId(item));
      return;
    }

    const [start, end] = anchorIndex <= itemIndex ? [anchorIndex, itemIndex] : [itemIndex, anchorIndex];
    const next = new Set(ids());
    for (let i = start; i <= end; i++) {
      next.add(getId(items[i]));
    }
    ids.set(next);
  }

  /** True só se a lista não estiver vazia e todo item estiver selecionado (usado pelo checkbox "selecionar tudo"). */
  function isAllSelected(items: T[]): boolean {
    return items.length > 0 && items.every(isSelected);
  }

  /** Se tudo já está selecionado, limpa; senão, seleciona todos os itens passados. */
  function toggleAll(items: T[]): void {
    ids.set(isAllSelected(items) ? new Set() : new Set(items.map(getId)));
    anchorId.set(null);
  }

  /** Zera a seleção (útil após uma remoção em lote). */
  function clear(): void {
    ids.set(new Set());
    anchorId.set(null);
  }

  return {
    selectedIds: ids.asReadonly(), // retorna uma referência para o próprio signal (não o valor atual do Set)
    selectedCount: computed(() => ids().size),
    isSelected,
    toggle,
    toggleWithRange,
    toggleAll,
    isAllSelected,
    clear,
  };
}
