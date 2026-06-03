import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { OutboxService, OutboxEntry, NewOutboxRequest } from './outbox.service';
import { IndexedDbService, ObjectStoreName } from './indexed-db.service';

/**
 * Stub IndexedDB en mémoire — évite une vraie base IndexedDB dans les tests.
 * On ne modélise que le store 'outbox' (clé = id).
 */
class InMemoryIndexedDbService {
  private outbox = new Map<string, OutboxEntry>();

  openDB(): Promise<unknown> {
    return Promise.resolve({});
  }
  get<T>(_store: ObjectStoreName, key: IDBValidKey): Promise<T | undefined> {
    return Promise.resolve(this.outbox.get(key as string) as T | undefined);
  }
  getAll<T>(): Promise<T[]> {
    return Promise.resolve([...this.outbox.values()] as T[]);
  }
  put<T>(_store: ObjectStoreName, value: T): Promise<void> {
    const entry = value as unknown as OutboxEntry;
    this.outbox.set(entry.id, entry);
    return Promise.resolve();
  }
  delete(_store: ObjectStoreName, key: IDBValidKey): Promise<void> {
    this.outbox.delete(key as string);
    return Promise.resolve();
  }
  clear(): Promise<void> {
    this.outbox.clear();
    return Promise.resolve();
  }
  deleteByTenant(_store: ObjectStoreName, tenantId: string): Promise<void> {
    for (const [id, e] of this.outbox.entries()) {
      if (e.tenantId === tenantId) {
        this.outbox.delete(id);
      }
    }
    return Promise.resolve();
  }
}

const baseRequest: NewOutboxRequest = {
  method: 'POST',
  url: '/api/v1/finance/invoices',
  body: { amount: 100 },
  headers: { 'Content-Type': 'application/json', 'X-Tenant-ID': 'acme' },
  tenantId: 'acme',
};

describe('OutboxService', () => {
  let service: OutboxService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        OutboxService,
        { provide: IndexedDbService, useClass: InMemoryIndexedDbService },
      ],
    });
    service = TestBed.inject(OutboxService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('should enqueue a mutating request with a generated idempotency key', async () => {
    const entry = await service.enqueue(baseRequest);
    expect(entry.idempotencyKey).toBeTruthy();
    expect(entry.status).toBe('PENDING');
    expect(service.pendingCount()).toBe(1);
    expect(service.entries()[0].url).toBe(baseRequest.url);
  });

  it('should NOT persist the Authorization header (no JWT at rest)', async () => {
    const entry = await service.enqueue({
      ...baseRequest,
      headers: { ...baseRequest.headers, Authorization: 'Bearer secret' },
    });
    expect(entry.headers['Authorization']).toBeUndefined();
    expect(entry.headers['Content-Type']).toBe('application/json');
  });

  it('should refuse to enqueue a denylisted (sensitive) endpoint', async () => {
    await expectAsync(
      service.enqueue({ ...baseRequest, url: '/api/v1/auth/2fa/verify' }),
    ).toBeRejected();
  });

  it('should remove an entry after a successful replay (drain)', fakeAsync(() => {
    void service.enqueue(baseRequest);
    tick();

    void service.sync();
    tick();

    const req = http.expectOne(baseRequest.url);
    expect(req.request.headers.get(OutboxService.IDEMPOTENCY_HEADER)).toBeTruthy();
    req.flush({ id: 'inv-1' });
    tick();

    expect(service.pendingCount()).toBe(0);
    expect(service.entries().length).toBe(0);
  }));

  it('should mark an entry CONFLICT on HTTP 409 and never drop it', fakeAsync(() => {
    void service.enqueue(baseRequest);
    tick();

    void service.sync();
    tick();

    const req = http.expectOne(baseRequest.url);
    req.flush({ detail: 'conflict' }, { status: 409, statusText: 'Conflict' });
    tick();

    const entries = service.entries();
    expect(entries.length).toBe(1);
    expect(entries[0].status).toBe('CONFLICT');
    expect(service.conflicts().length).toBe(1);
  }));

  it('should retry with PENDING on transient error then cap at MAX_RETRIES (FAILED)', fakeAsync(() => {
    void service.enqueue(baseRequest);
    tick();

    // Replay enough times to exhaust the retry budget.
    for (let attempt = 0; attempt < OutboxService.MAX_RETRIES; attempt++) {
      void service.sync();
      tick();
      const req = http.expectOne(baseRequest.url);
      req.flush({ detail: 'boom' }, { status: 500, statusText: 'Server Error' });
      tick();
    }

    const entry = service.entries()[0];
    expect(entry.retries).toBe(OutboxService.MAX_RETRIES);
    expect(entry.status).toBe('FAILED');

    // Once capped, a further sync must NOT replay it again.
    void service.sync();
    tick();
    http.expectNone(baseRequest.url);
  }));

  it('should compute an exponential, capped backoff delay', () => {
    expect(OutboxService.backoffDelayMs(0)).toBe(1000);
    expect(OutboxService.backoffDelayMs(1)).toBe(2000);
    expect(OutboxService.backoffDelayMs(2)).toBe(4000);
    expect(OutboxService.backoffDelayMs(100)).toBe(60000); // capped
  });

  it('should clearForTenant removing only that tenant entries', fakeAsync(() => {
    void service.enqueue(baseRequest);
    void service.enqueue({ ...baseRequest, tenantId: 'other' });
    tick();
    expect(service.entries().length).toBe(2);

    void service.clearForTenant('acme');
    tick();

    const remaining = service.entries();
    expect(remaining.length).toBe(1);
    expect(remaining[0].tenantId).toBe('other');
  }));

  it('should clearAll wiping the queue', fakeAsync(() => {
    void service.enqueue(baseRequest);
    tick();
    void service.clearAll();
    tick();
    expect(service.entries().length).toBe(0);
  }));
});
