// Helper to get either the user-selected location or the IP-derived location from backend.
// Usage example (Leaflet):
//   getLocationOrSelected(selectedLocation).then(loc => {
//     map.setView([loc.lat, loc.lon], 13);
//     loadTrendingAround(loc.lat, loc.lon, radius);
//   });

async function getLocationOrSelected(selected) {
  if (selected && typeof selected.lat === 'number' && typeof selected.lon === 'number') {
    return selected;
  }

  try {
    const resp = await fetch('/api/geoip');
    if (!resp.ok) {
      console.debug('GeoIP fetch failed:', resp.status);
      throw new Error('GeoIP fetch failed');
    }
    const json = await resp.json();
    if (json && typeof json.lat === 'number' && typeof json.lon === 'number') {
      return { lat: json.lat, lon: json.lon };
    }
    throw new Error(json && json.error ? json.error : 'No coordinates');
  } catch (e) {
    // Fallback: try browser geolocation if available
    if (navigator.geolocation) {
      return new Promise((resolve, reject) => {
        navigator.geolocation.getCurrentPosition(pos => {
          resolve({ lat: pos.coords.latitude, lon: pos.coords.longitude });
        }, err => {
          reject(err);
        }, { timeout: 5000 });
      });
    }
    throw e;
  }
}

// Export for modules or attach to window
if (typeof module !== 'undefined' && module.exports) {
  module.exports = { getLocationOrSelected };
} else {
  window.getLocationOrSelected = getLocationOrSelected;
}

