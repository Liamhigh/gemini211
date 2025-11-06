const CACHE_NAME = 'verum-omnis-cache-v4';
const urlsToCache = [
  '/',
  '/index.html',
  '/index.tsx',
  '/favicon.svg',
  '/manifest.json',
  '/icon-192.png',
  '/icon-512.png'
];

self.addEventListener('install', event => {
  self.skipWaiting(); // Ensure the new service worker activates immediately
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then(cache => {
        console.log('Opened cache');
        return cache.addAll(urlsToCache);
      })
  );
});

self.addEventListener('activate', event => {
  const cacheWhitelist = [CACHE_NAME];
  event.waitUntil(
    caches.keys().then(cacheNames => {
      return Promise.all(
        cacheNames.map(cacheName => {
          if (cacheWhitelist.indexOf(cacheName) === -1) {
            return caches.delete(cacheName);
          }
        })
      );
    }).then(() => self.clients.claim()) // Take control of open clients
  );
});

self.addEventListener('fetch', event => {
  const requestUrl = new URL(event.request.url);

  // Only apply network-first strategy to our own app shell assets.
  // Other requests (API, CDNs) will be handled by the browser as normal.
  if (urlsToCache.includes(requestUrl.pathname) || requestUrl.pathname === '/') {
    event.respondWith(
      fetch(event.request)
        .then(networkResponse => {
          // If the fetch is successful, update the cache and return the response.
          return caches.open(CACHE_NAME).then(cache => {
            if (networkResponse.status === 200) {
              cache.put(event.request, networkResponse.clone());
            }
            return networkResponse;
          });
        })
        .catch(() => {
          // If the network request fails (offline), serve from the cache.
          return caches.match(event.request);
        })
    );
  }
});
