import http from "k6/http";
import { check, sleep } from "k6";

const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";
const JWT_TOKEN = __ENV.JWT_TOKEN || "";
const RADIUS_METERS = Number(__ENV.RADIUS_METERS || 200);
const VUS = Number(__ENV.VUS || 20);
const DURATION = __ENV.DURATION || "30s";
const SCENARIO = (__ENV.SCENARIO || "both").toLowerCase();

// Locations: default hotspot (Novi Sad), can be overridden via env vars.
const HOTSPOT = {
  name: __ENV.HOTSPOT_NAME || "Novi Sad (center)",
  lat: Number(__ENV.HOTSPOT_LAT || 45.2671),
  lng: Number(__ENV.HOTSPOT_LNG || 19.8335),
};

const DISTRIBUTED = [
  { name: "Novi Sad", lat: 45.2671, lng: 19.8335 },
  { name: "Beograd", lat: 44.7866, lng: 20.4489 },
  { name: "Nis", lat: 43.3209, lng: 21.8958 },
  { name: "Subotica", lat: 46.1004, lng: 19.6650 },
];

if (!JWT_TOKEN) {
  // k6 will print this at the start; run still continues but will fail auth checks.
  console.warn("JWT_TOKEN is empty. Trending endpoint requires auth.");
}

export const options = buildOptions();

function buildOptions() {
  const base = {
    thresholds: {
      http_req_failed: ["rate<0.01"],
      http_req_duration: ["p(95)<1000"],
    },
  };

  if (SCENARIO === "hotspot") {
    return {
      ...base,
      scenarios: {
        hotspot: {
          executor: "constant-vus",
          vus: VUS,
          duration: DURATION,
          exec: "hotspot",
        },
      },
    };
  }

  if (SCENARIO === "distributed") {
    return {
      ...base,
      scenarios: {
        distributed: {
          executor: "constant-vus",
          vus: VUS,
          duration: DURATION,
          exec: "distributed",
        },
      },
    };
  }

  return {
    ...base,
    scenarios: {
      hotspot: {
        executor: "constant-vus",
        vus: VUS,
        duration: DURATION,
        exec: "hotspot",
      },
      distributed: {
        executor: "constant-vus",
        vus: VUS,
        duration: DURATION,
        exec: "distributed",
        startTime: "5s",
      },
    },
  };
}

function trendingRequest(lat, lng) {
  const url =
    `${BASE_URL}/api/trending?lat=${lat}` +
    `&lng=${lng}&radiusMeters=${RADIUS_METERS}`;

  const res = http.get(url, {
    headers: {
      Authorization: `Bearer ${JWT_TOKEN}`,
    },
  });

  check(res, {
    "status 200": (r) => r.status === 200,
  });
}

export function hotspot() {
  trendingRequest(HOTSPOT.lat, HOTSPOT.lng);
  sleep(0.2);
}

export function distributed() {
  const idx = Math.floor(Math.random() * DISTRIBUTED.length);
  const loc = DISTRIBUTED[idx];
  trendingRequest(loc.lat, loc.lng);
  sleep(0.2);
}
