// Brand splash (Android BrandSplash): the painted scene and a loading bar, 1 s the first time and 0.6 s after that - longer only while the first scene is still loading.
(function () {
  var sp = document.getElementById("splash"), img = sp.firstChild, bar = document.getElementById("sp-bar"), fill = bar.firstChild;
  var seen = false; try { seen = localStorage.getItem("splashSeen") === "1"; } catch (e) {}
  var total = seen ? 600 : 1000, t0 = Date.now(), done = false;
  function lay() {
    var w = innerWidth, h = innerHeight, k = Math.max(w / 841, h / 1870), ox = (w - 841 * k) / 2, oy = (h - 1870 * k) / 2;
    bar.style.left = ox + 196 * k + "px"; bar.style.top = oy + 1354 * k + "px"; bar.style.width = 446 * k + "px"; bar.style.height = 76 * k + "px"; bar.style.padding = 0.17 * 76 * k + "px";
  }
  lay(); addEventListener("resize", lay);
  // The bar only finishes once the first scene's picture and the lettering font are really in, so the game never opens half drawn
  // (the longest wait is 6 s, for a very slow line).
  window.__appReady = false;
  var need = 2, rel = function () { if (--need === 0) window.__appReady = true; };
  var first = new Image(); first.onload = first.onerror = rel; first.src = "img/bg_home_scene.webp";
  (document.fonts && document.fonts.ready ? document.fonts.ready : Promise.resolve()).then(rel, rel);
  setTimeout(function () { window.__appReady = true; }, 6000);
  function end() { if (done) return; done = true; try { localStorage.setItem("splashSeen", "1"); } catch (e) {} sp.style.opacity = "0"; setTimeout(function () { sp.remove(); }, 300); }
  sp.addEventListener("click", function () { total = 170; t0 = Date.now() - (Date.now() - t0); });
  (function tick() {
    var p = Math.min(1, (Date.now() - t0) / total), e = p < .5 ? 2 * p * p : 1 - Math.pow(-2 * p + 2, 2) / 2;
    fill.style.width = (e * 100) + "%"; img.style.transform = "scale(" + (1 + .05 * e) + ")";
    if (p >= 1 && window.__appReady !== false) return end();
    requestAnimationFrame(tick);
  })();
})();
