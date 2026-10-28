// The legal pages hold every language in one document, one <section data-lang> each. This shows the one asked for (?lang=xx),
// else the one this browser prefers, else Turkish; without JavaScript every section simply shows, one after another.
(function () {
  var root = document.documentElement, langs = ["tr", "en", "pt", "es"];
  function pick() {
    var q = (location.search.match(/[?&]lang=([a-z]{2})/) || [])[1];
    if (q && langs.indexOf(q) >= 0) return q;
    var h = location.hash.replace("#", "");
    if (langs.indexOf(h) >= 0) return h;
    var nav = navigator.languages || [navigator.language || ""];
    for (var i = 0; i < nav.length; i++) { var c = String(nav[i]).slice(0, 2).toLowerCase(); if (langs.indexOf(c) >= 0) return c; }
    return "tr";
  }
  function show(l) {
    var s = document.querySelectorAll("section[data-lang]");
    for (var i = 0; i < s.length; i++) s[i].classList.toggle("on", s[i].getAttribute("data-lang") === l);
    var a = document.querySelectorAll("nav.langs a");
    for (var j = 0; j < a.length; j++) a[j].setAttribute("aria-current", a[j].getAttribute("data-l") === l ? "true" : "false");
    root.lang = l;
    var sec = document.querySelector('section[data-lang="' + l + '"]'), t = sec && sec.getAttribute("data-title");
    if (t) document.title = t;
  }
  root.classList.add("js");
  document.addEventListener("click", function (e) {
    var a = e.target.closest && e.target.closest("nav.langs a");
    if (!a) return;
    e.preventDefault(); show(a.getAttribute("data-l"));
    try { history.replaceState(null, "", "?lang=" + a.getAttribute("data-l")); } catch (x) {}
  });
  show(pick());
})();
