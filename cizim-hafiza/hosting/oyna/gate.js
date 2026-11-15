// The browser game is for iPhones and iPads only. Everyone else (Android above all) is sent to the download page: the game's
// income is the ads inside the Android app, so it is the app that has to be played there. Loaded first, as a plain script.
(function () {
  var ua = navigator.userAgent || "";
  var ios = /iPhone|iPad|iPod/i.test(ua) || (navigator.platform === "MacIntel" && navigator.maxTouchPoints > 1);
  // A door for testing the browser game on another phone: opening the game or the download page with ?webtest=1 lets this browser in (it
  // is remembered), ?webtest=0 shuts it again. Nothing on the site links to it, so everybody else still lands on the download page.
  var test = false;
  try {
    var m = /[?&]webtest=([01])(&|$)/.exec(location.search);
    if (m) localStorage.setItem("karalak_webtest", m[1]);
    test = localStorage.getItem("karalak_webtest") === "1";
  } catch (e) { test = /[?&]webtest=1(&|$)/.test(location.search); }
  if (!ios && !test) location.replace("/indir" + location.search);
})();
