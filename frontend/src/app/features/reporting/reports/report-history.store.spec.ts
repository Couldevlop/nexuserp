import { TestBed } from '@angular/core/testing';
import { ReportHistoryStore } from './report-history.store';
import { ReportDto } from './reporting-format';

function dto(over: Partial<ReportDto> = {}): ReportDto {
  return {
    id: 'r-1',
    type: 'BALANCE_SHEET',
    status: 'PROCESSING',
    downloadUrl: null,
    errorMessage: null,
    requestedAt: '2026-06-01T10:00:00',
    completedAt: null,
    ...over,
  };
}

describe('ReportHistoryStore', () => {
  let store: ReportHistoryStore;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({ providers: [ReportHistoryStore] });
    store = TestBed.inject(ReportHistoryStore);
  });

  afterEach(() => localStorage.clear());

  it('should return empty list initially', () => {
    expect(store.list()).toEqual([]);
  });

  it('should upsert and persist a report', () => {
    store.upsert(dto());
    expect(store.list().length).toBe(1);
    expect(store.list()[0].id).toBe('r-1');
  });

  it('should replace an existing report on upsert (same id)', () => {
    store.upsert(dto());
    store.upsert(dto({ status: 'COMPLETED', downloadUrl: 'u' }));
    const all = store.list();
    expect(all.length).toBe(1);
    expect(all[0].status).toBe('COMPLETED');
  });

  it('should sort by requestedAt descending', () => {
    store.upsert(dto({ id: 'a', requestedAt: '2026-01-01T00:00:00' }));
    store.upsert(dto({ id: 'b', requestedAt: '2026-06-01T00:00:00' }));
    expect(store.list()[0].id).toBe('b');
  });

  it('should remove a report by id', () => {
    store.upsert(dto());
    store.remove('r-1');
    expect(store.list()).toEqual([]);
  });

  it('should tolerate corrupted localStorage content', () => {
    localStorage.setItem('nexuserp.reporting.jobs', '{not-json');
    expect(store.list()).toEqual([]);
  });

  it('should ignore non-array persisted payloads', () => {
    localStorage.setItem('nexuserp.reporting.jobs', '{"a":1}');
    expect(store.list()).toEqual([]);
  });
});
