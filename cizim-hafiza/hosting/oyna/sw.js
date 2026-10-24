// Caches pictures, fonts and music after the first visit so the site opens fast next time.
// Pages, scripts and data always come from the network (firebase.json sends them with no-cache), so updates are never stuck.
const CACHE = "karalak-assets-v1";
self.addEventListener("install", () => self.skipWaiting());
self.addEventListener("activate", (e) => e.waitUntil((async () => { for (const k of await caches.keys()) if (k !== CACHE) await caches.delete(k); await self.clients.claim(); })()));
self.addEventListener("fetch", (e) => {
  const u = new URL(e.request.url);
  if (e.request.method !== "GET" || u.origin !== location.origin || !/\.(webp|png|jpg|svg|ttf|woff2|m4a|mp3|mp4)$/.test(u.pathname)) return;
  // Pictures are served from the phone at once (they are kept for a year); a quiet re-check in the background picks up a changed
  // file for the next visit, so a replaced picture still arrives without a version number in its name.
  e.respondWith((async () => {
    const c = await caches.open(CACHE), hit = await c.match(e.request, { ignoreSearch: false });
    const fresh = fetch(e.request, { cache: "no-cache" }).then((res) => { if (res.ok && res.status === 200) c.put(e.request, res.clone()); return res; }).catch(() => null);
    if (hit) { e.waitUntil(fresh); return hit; }
    return (await fresh) || fetch(e.request);
  })());
});
