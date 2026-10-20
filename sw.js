// BiteWise service worker: lets the app open offline.
const CACHE = "bitewise-v25";
const SHELL = ["./", "./index.html", "./manifest.json", "./icon-192.png", "./icon-512.png",
  "./vendor/html5-qrcode/html5-qrcode.min.js",
  "./vendor/archivo/archivo-latin-wdth-normal.woff2", "./vendor/archivo/archivo-latin-ext-wdth-normal.woff2"];

self.addEventListener("install", e => {
  e.waitUntil(caches.open(CACHE).then(c => c.addAll(SHELL)).then(() => self.skipWaiting()));
});

self.addEventListener("activate", e => {
  e.waitUntil(caches.keys().then(keys => Promise.all(keys.filter(k => k !== CACHE).map(k => caches.delete(k))))
    .then(() => self.clients.claim()));
});

self.addEventListener("fetch", e => {
  const req = e.request;
  if (req.method !== "GET") return;
  const url = new URL(req.url);
  // Food lookups always go to the internet.
  if (url.hostname.endsWith("openfoodfacts.org") || url.hostname.endsWith("nal.usda.gov")) return;
  // Try the network first so updates show up, fall back to the saved copy when offline.
  // For the app's own files, always check GitHub for a newer copy (skip the browser's short-term cache).
  const net = url.origin === location.origin ? fetch(url.href, { cache: "no-cache" }) : fetch(req);
  e.respondWith(
    net.then(res => {
      if (res.ok && url.origin === location.origin) {
        const copy = res.clone();
        caches.open(CACHE).then(c => c.put(req, copy));
      }
      return res;
    }).catch(() => caches.match(req).then(r => r || (req.mode === "navigate" ? caches.match("./index.html") : undefined)))
  );
});
