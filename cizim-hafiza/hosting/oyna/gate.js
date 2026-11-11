// The browser game is for iPhones and iPads only. Everyone else (Android above all) is sent to the download page: the game's
// income is the ads inside the Android app, so it is the app that has to be played there. Loaded first, as a plain script.
(function () {
  var ua = navigator.userAgent || "";
  var ios = /iPhone|iPad|iPod/i.test(ua) || (navigator.platform === "MacIntel" && navigator.maxTouchPoints > 1);
  if (!ios) location.replace("/indir" + location.search);
})();
