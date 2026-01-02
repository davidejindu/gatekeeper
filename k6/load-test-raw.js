import http from 'k6/http';
import { check } from 'k6';

export const options = {
  scenarios: {
    constant_load: {
      executor: 'constant-arrival-rate',
      rate: 20000,
      timeUnit: '1s',
      duration: '30s',
      preAllocatedVUs: 400,
      maxVUs: 800,
    },
  },
  thresholds: {
    http_req_duration: ['p(99)<10'],
  },
};

export default function () {
  const res = http.get('http://localhost:8080/api/health', {  // ← Add /api
      headers: { 'X-API-Key': 'test-key-1' },
  });

  check(res, {
    'status 200': (r) => r.status === 200
  });
}