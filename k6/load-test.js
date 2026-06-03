/**
 * NexusERP — k6 Load Test Suite
 * Covers: API Gateway, Finance, Authentication flows
 * Run: k6 run --env BASE_URL=http://localhost:8080 k6/load-test.js
 */
import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';
import { randomItem, randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

// ─── Custom metrics ───────────────────────────────────────────────────────────
const errorRate        = new Rate('nexuserp_errors');
const invoiceCreation  = new Trend('nexuserp_invoice_creation_ms');
const invoiceList      = new Trend('nexuserp_invoice_list_ms');
const dashboardLoad    = new Trend('nexuserp_dashboard_load_ms');
const aiChatLatency    = new Trend('nexuserp_ai_chat_ms');
const requestsTotal    = new Counter('nexuserp_requests_total');

// ─── Configuration ────────────────────────────────────────────────────────────
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const AI_URL   = __ENV.AI_URL   || 'http://localhost:8001';
const TOKEN    = __ENV.JWT_TOKEN || 'test-token-replace-me';

const TENANTS = ['fr-acme', 'ci-sarl', 'fr-startup'];

// ─── Test scenarios ───────────────────────────────────────────────────────────
export const options = {
  scenarios: {
    // Smoke test — verify everything works
    smoke: {
      executor: 'constant-vus',
      vus: 1,
      duration: '30s',
      tags: { scenario: 'smoke' },
      exec: 'smokeTest',
    },
    // Load test — normal traffic
    load: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '2m', target: 50 },   // Ramp up
        { duration: '5m', target: 50 },   // Steady state
        { duration: '2m', target: 100 },  // Peak
        { duration: '5m', target: 100 },  // Sustained peak
        { duration: '2m', target: 0 },    // Ramp down
      ],
      tags: { scenario: 'load' },
      exec: 'loadTest',
      startTime: '35s',
    },
    // Stress test — find breaking point
    stress: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '2m', target: 100 },
        { duration: '2m', target: 200 },
        { duration: '2m', target: 300 },
        { duration: '2m', target: 400 },
        { duration: '2m', target: 0 },
      ],
      tags: { scenario: 'stress' },
      exec: 'stressTest',
      startTime: '20m',
    },
    // Spike test — sudden burst
    spike: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 10 },
        { duration: '10s', target: 500 },  // Sudden spike
        { duration: '30s', target: 10 },
        { duration: '10s', target: 0 },
      ],
      tags: { scenario: 'spike' },
      exec: 'spikeTest',
      startTime: '35m',
    },
  },
  thresholds: {
    // SLA requirements
    http_req_duration: [
      'p(95)<500',    // 95% of requests under 500ms
      'p(99)<2000',   // 99% under 2s
    ],
    http_req_failed: ['rate<0.01'],  // Error rate < 1%
    nexuserp_errors: ['rate<0.01'],
    nexuserp_invoice_creation_ms: ['p(95)<1000'],
    nexuserp_invoice_list_ms: ['p(95)<300'],
    nexuserp_dashboard_load_ms: ['p(95)<200'],
    nexuserp_ai_chat_ms: ['p(95)<5000'],  // AI allowed more latency
  },
};

// ─── Shared headers ───────────────────────────────────────────────────────────
function authHeaders(tenantId) {
  return {
    'Authorization': `Bearer ${TOKEN}`,
    'X-Tenant-ID': tenantId,
    'Content-Type': 'application/json',
  };
}

// ─── Smoke test ───────────────────────────────────────────────────────────────
export function smokeTest() {
  const tenant = 'fr-acme';
  const headers = authHeaders(tenant);

  group('Smoke — health checks', () => {
    const r = http.get(`${BASE_URL}/actuator/health`);
    check(r, { 'gateway health OK': (r) => r.status === 200 });
  });

  group('Smoke — invoice list', () => {
    const r = http.get(`${BASE_URL}/api/v1/finance/invoices?page=0&size=5`, { headers });
    check(r, {
      'list status 200': (r) => r.status === 200,
      'has data field': (r) => JSON.parse(r.body).data !== undefined,
    });
    errorRate.add(r.status !== 200);
    invoiceList.add(r.timings.duration);
  });

  requestsTotal.add(2);
  sleep(1);
}

// ─── Load test ────────────────────────────────────────────────────────────────
export function loadTest() {
  const tenant = randomItem(TENANTS);
  const headers = authHeaders(tenant);

  group('Dashboard KPIs', () => {
    const r = http.get(`${BASE_URL}/api/v1/finance/invoices?status=SUBMITTED&size=5`, { headers });
    check(r, { 'dashboard 200': (r) => r.status === 200 });
    dashboardLoad.add(r.timings.duration);
    errorRate.add(r.status >= 400 ? 1 : 0);
    requestsTotal.add(1);
  });

  group('Invoice list with pagination', () => {
    const page = randomIntBetween(0, 5);
    const r = http.get(
      `${BASE_URL}/api/v1/finance/invoices?page=${page}&size=20&sort=invoiceDate,desc`,
      { headers }
    );
    check(r, {
      'invoice list 200': (r) => r.status === 200,
      'pagination present': (r) => {
        const body = JSON.parse(r.body);
        return body.meta && body.meta.page !== undefined;
      },
    });
    invoiceList.add(r.timings.duration);
    errorRate.add(r.status >= 400 ? 1 : 0);
    requestsTotal.add(1);
  });

  // ~30% of users create invoices
  if (Math.random() < 0.3) {
    group('Create invoice', () => {
      const payload = JSON.stringify({
        customerName: `Load Test Client ${randomIntBetween(1, 9999)}`,
        customerId: `cust-lt-${randomIntBetween(1, 1000)}`,
        currency: tenant.startsWith('ci') ? 'XOF' : 'EUR',
        taxRate: tenant.startsWith('ci') ? 18 : 20,
        invoiceDate: new Date().toISOString().split('T')[0],
        dueDate: new Date(Date.now() + 30 * 86400000).toISOString().split('T')[0],
        lines: [{
          description: 'Load test service',
          quantity: randomIntBetween(1, 10),
          unitPrice: randomIntBetween(100, 10000),
          discountPercent: 0,
          taxRate: tenant.startsWith('ci') ? 18 : 20,
        }],
      });

      const r = http.post(`${BASE_URL}/api/v1/finance/invoices`, payload, { headers });
      check(r, {
        'invoice created 201': (r) => r.status === 201,
        'has invoice ID': (r) => {
          const body = JSON.parse(r.body);
          return body.data && body.data.id;
        },
      });
      invoiceCreation.add(r.timings.duration);
      errorRate.add(r.status !== 201 ? 1 : 0);
      requestsTotal.add(1);
    });
  }

  // ~10% use AI assistant
  if (Math.random() < 0.1) {
    group('AI chat assistant', () => {
      const aiHeaders = { ...headers };
      const payload = JSON.stringify({
        message: 'Montre-moi les factures en retard de ce mois',
        tenant_id: tenant,
        module: 'finance',
        user_role: 'FINANCE_USER',
        country: tenant.startsWith('ci') ? 'CI' : 'FR',
      });

      const r = http.post(`${AI_URL}/ai/v1/chat`, payload, { headers: aiHeaders });
      check(r, {
        'AI chat 200': (r) => r.status === 200,
        'has response': (r) => JSON.parse(r.body).response !== undefined,
      });
      aiChatLatency.add(r.timings.duration);
      errorRate.add(r.status !== 200 ? 1 : 0);
      requestsTotal.add(1);
    });
  }

  sleep(randomIntBetween(1, 3));
}

// ─── Stress test ──────────────────────────────────────────────────────────────
export function stressTest() {
  const tenant = randomItem(TENANTS);
  const headers = authHeaders(tenant);

  // Lighter scenario — focus on read operations
  const r = http.get(
    `${BASE_URL}/api/v1/finance/invoices?page=0&size=10`,
    { headers }
  );
  check(r, { 'stress 200': (r) => r.status === 200 });
  errorRate.add(r.status >= 400 ? 1 : 0);
  requestsTotal.add(1);
  sleep(0.5);
}

// ─── Spike test ───────────────────────────────────────────────────────────────
export function spikeTest() {
  const tenant = randomItem(TENANTS);
  const headers = authHeaders(tenant);

  const r = http.get(`${BASE_URL}/actuator/health`);
  check(r, { 'spike health 200': (r) => r.status === 200 });
  errorRate.add(r.status !== 200 ? 1 : 0);
  requestsTotal.add(1);
}

// ─── Teardown ─────────────────────────────────────────────────────────────────
export function teardown(data) {
  console.log(`Load test completed. Total requests: ${requestsTotal.name}`);
}
