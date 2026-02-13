import http from "k6/http";
import { check, sleep } from "k6";

export const options = {
  scenarios: {
    steady_rps: {
      executor: "constant-arrival-rate",
      rate: 250,
      timeUnit: "1s",
      duration: "60s",
      preAllocatedVUs: 50,
      maxVUs: 200
    }
  }
};

const baseUrl = __ENV.TARGET_URL || "http://localhost:8080/api/whoami";
const authToken = __ENV.AUTH_TOKEN;

export default function () {
  const params = authToken
    ? { headers: { Authorization: `Bearer ${authToken}` } }
    : {};
  const res = http.get(baseUrl, params);
  check(res, { "status is 200": (r) => r.status === 200 });
  sleep(0.1);
}
