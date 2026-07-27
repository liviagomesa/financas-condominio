import { Signal, computed, signal } from '@angular/core';

export interface Selection<T> {
  readonly selectedIds: Signal<ReadonlySet<number>>;
  readonly selectedCount: Signal<number>;
  isSelected(item: T): boolean;
  toggle(item: T): void;
  toggleAll(items: T[]): void;
  isAllSelected(items: T[]): boolean;
  clear(): void;
}

export function createSelection<T>(getId: (item: T) => number): Selection<T> {
  const ids = signal<ReadonlySet<number>>(new Set());

  function isSelected(item: T): boolean {
    return ids().has(getId(item));
  }

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

  function isAllSelected(items: T[]): boolean {
    return items.length > 0 && items.every(isSelected);
  }

  function toggleAll(items: T[]): void {
    ids.set(isAllSelected(items) ? new Set() : new Set(items.map(getId)));
  }

  function clear(): void {
    ids.set(new Set());
  }

  return {
    selectedIds: ids.asReadonly(),
    selectedCount: computed(() => ids().size),
    isSelected,
    toggle,
    toggleAll,
    isAllSelected,
    clear,
  };
}
