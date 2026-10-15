// Caches pictures, fonts and music after the first visit so the site opens fast next time.
// Pages, scripts and data always come from the network (firebase.json sends them with no-cache), so updates are never stuck.
const CACHE = "karalak-assets-v1";
self.addEventListener("install", () => self.skipWaiting());
self.addEventListener("activate", (e) => e.waitUntil((async () => { for (const k of await caches.keys()) if (k !== CACHE) await caches.delete(k); await self.clients.claim(); })()));
self.addEventListener("fetch", (e) => {
  const u = new URL(e.request.url);
  if (e.request.method !== "GET" || u.origin !== location.origin || !/\.(webp|png|jpg|ttf|m4a)$/.test(u.pathname)) return;
  e.respondWith((async () => {
    const c = await caches.open(CACHE), hit = await c.match(e.request);
    if (hit) return hit;
    const res = await fetch(e.request);
    if (res.ok && res.status === 200) c.put(e.request, res.clone());
    return res;
  })());
});
