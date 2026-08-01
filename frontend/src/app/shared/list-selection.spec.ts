import { describe, expect, it } from 'vitest';
import { createSelection } from './list-selection';

interface Item {
  id: number;
}

const items: Item[] = [{ id: 1 }, { id: 2 }, { id: 3 }, { id: 4 }, { id: 5 }, { id: 6 }];

describe('createSelection - toggleWithRange', () => {
  it('behaves like a normal click and sets the anchor when shiftKey is false', () => {
    const selection = createSelection<Item>((item) => item.id);
    selection.toggleWithRange(items[1], items, false);
    expect(Array.from(selection.selectedIds())).toEqual([2]);
  });

  it('behaves like a normal click when shiftKey is true but there is no anchor yet', () => {
    const selection = createSelection<Item>((item) => item.id);
    selection.toggleWithRange(items[3], items, true);
    expect(Array.from(selection.selectedIds())).toEqual([4]);
  });

  it('marks every row in the interval between the anchor and the clicked row (anchor before item)', () => {
    const selection = createSelection<Item>((item) => item.id);
    selection.toggleWithRange(items[1], items, false); // anchor = id 2
    selection.toggleWithRange(items[5], items, true); // shift+click id 6
    expect(Array.from(selection.selectedIds()).sort()).toEqual([2, 3, 4, 5, 6]);
  });

  it('marks every row in the interval between the anchor and the clicked row (anchor after item)', () => {
    const selection = createSelection<Item>((item) => item.id);
    selection.toggleWithRange(items[4], items, false); // anchor = id 5
    selection.toggleWithRange(items[0], items, true); // shift+click id 1
    expect(Array.from(selection.selectedIds()).sort()).toEqual([1, 2, 3, 4, 5]);
  });

  it('does not unmark rows outside the interval that were already selected', () => {
    const selection = createSelection<Item>((item) => item.id);
    selection.toggle(items[5]); // pre-existing selection, id 6, outside the upcoming interval
    selection.toggleWithRange(items[1], items, false); // anchor = id 2
    selection.toggleWithRange(items[3], items, true); // shift+click id 4
    expect(Array.from(selection.selectedIds()).sort()).toEqual([2, 3, 4, 6]);
  });

  it('keeps the anchor fixed across successive shift+clicks', () => {
    const selection = createSelection<Item>((item) => item.id);
    selection.toggleWithRange(items[1], items, false); // anchor = id 2
    selection.toggleWithRange(items[3], items, true); // shift+click id 4 -> 2..4
    selection.toggleWithRange(items[5], items, true); // shift+click id 6 -> still anchored at 2 -> 2..6
    expect(Array.from(selection.selectedIds()).sort()).toEqual([2, 3, 4, 5, 6]);
  });

  it('resets the anchor on toggleAll, so the next shift+click behaves like a normal click', () => {
    const selection = createSelection<Item>((item) => item.id);
    selection.toggleWithRange(items[1], items, false); // anchor = id 2
    selection.toggleAll(items);
    selection.clear();
    selection.toggleWithRange(items[4], items, true); // shift+click without anchor -> normal click
    expect(Array.from(selection.selectedIds())).toEqual([5]);
  });

  it('resets the anchor on clear, so the next shift+click behaves like a normal click', () => {
    const selection = createSelection<Item>((item) => item.id);
    selection.toggleWithRange(items[1], items, false); // anchor = id 2
    selection.clear();
    selection.toggleWithRange(items[4], items, true); // shift+click without anchor -> normal click
    expect(Array.from(selection.selectedIds())).toEqual([5]);
  });
});
