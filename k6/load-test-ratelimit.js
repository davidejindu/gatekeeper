import http from 'k6/http';
import { check } from 'k6';

const API_KEYS = Array.from({ length: 100 }, (_, i) => `test-key-${i + 1}`);

export const options = {
  scenarios: {
    concurrent_users: {
      executor: 'constant-arrival-rate',
      rate: 10000,
      timeUnit: '1s',
      duration: '60s',
      preAllocatedVUs: 200,
      maxVUs: 400,
    },
  },
};

export default function () {
  const apiKey = API_KEYS[Math.floor(Math.random() * API_KEYS.length)];
  
  const res = http.get('http://localhost:8080/api/test', {
    headers: { 'X-API-Key': apiKey },
  });

  check(res, {
    'valid response': (r) => r.status === 200 || r.status === 429,
  });
}