import { signal, computed } from '@angular/core';

export const PAGE_SIZE = 10;

/** Creates a self-contained paginator for a readonly list signal. */
export function paginate<T>(
  sourceSignal: () => T[],
  pageSize = PAGE_SIZE
) {
  const page = signal(1);

  const pageCount = computed(() => Math.max(1, Math.ceil(sourceSignal().length / pageSize)));
  const paged     = computed(() => {
    const p = Math.min(page(), pageCount()); // clamp
    return sourceSignal().slice((p - 1) * pageSize, p * pageSize);
  });
  const range     = computed(() => {
    const total = sourceSignal().length;
    if (total === 0) return '0 results';
    const p = Math.min(page(), pageCount());
    const from = (p - 1) * pageSize + 1;
    const to   = Math.min(p * pageSize, total);
    return `${from}–${to} of ${total}`;
  });

  function go(n: number) { page.set(Math.max(1, Math.min(n, pageCount()))); }
  function reset()       { page.set(1); }
  function prev()        { go(page() - 1); }
  function next()        { go(page() + 1); }
  function first()       { page.set(1); }
  function last()        { page.set(pageCount()); }

  return { page, pageCount, paged, range, go, reset, prev, next, first, last };
}
