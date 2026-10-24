// The phone's own back button (and the browser's back gesture). The game is one page, so by default that button leaves the whole site.
// Here it behaves like the app's: it closes an open window, goes back one screen, asks before leaving a game, and on the home screen
// asks "Çıkmak istiyor musun?" instead of closing Karalak at once.
import { exitDialog } from "./ui.js";
import { S } from "./i18n.js";

let leaving = false, armed = false;

export function initPhoneBack(c) {
  try { history.replaceState({ k: "base" }, ""); } catch { return; }
  // Chrome skips history entries a page adds before the player has touched it, so the guard entry is added at the first touch
  const arm = () => { if (armed) return; armed = true; try { history.pushState({ k: "app" }, ""); } catch {} };
  for (const ev of ["pointerdown", "touchstart", "keydown"]) addEventListener(ev, arm, { once: true, capture: true, passive: true });
  addEventListener("popstate", () => {
    if (leaving || !armed) return;
    try { history.pushState({ k: "app" }, ""); } catch {} // stay on the guarded entry, so the next press is caught too
    back(c);
  });
}

const visible = (e) => !!e && !e.classList.contains("hidden") && e.offsetParent !== null;

function back(c) {
  const { $ } = c, cur = c.current();
  // a chest scene that is still playing is never cut short; the reward screen's button is the way out of it
  if (document.querySelector(".os2")) return;
  const won = document.querySelector(".won .won-btn"); if (won) { won.click(); return; }
  // windows first, top one only: the painted windows close when their dimmed backdrop is tapped
  const wins = document.querySelectorAll(".ex-ov, .aw-ov, .pc-ov"); if (wins.length) { wins[wins.length - 1].click(); return; }
  if (visible($("dialog"))) { const no = $("dlg-no"); (visible(no) ? no : $("dialog")).click(); return; }
  if (visible($("pick"))) return; // the login-streak window is answered with its own button
  const sheet = $("ow-sheet"); if (visible(sheet)) { sheet.click(); return; }
  const menu = $("fr-menu"); if (visible(menu)) { menu.click(); return; }
  if (cur === "home") {
    exitDialog($("stage"), {
      style: "menu", title: S("exit_confirm_title"), message: S("exit_confirm_message"), confirm: S("exit_confirm_yes"), dismiss: S("exit_confirm_no"),
      // two steps back: past the guarded entry and the page's own, which leaves for whatever was open before Karalak
      onConfirm: () => { leaving = true; history.go(-2); setTimeout(() => { leaving = false; }, 600); },
    });
    return;
  }
  // a game that is on (drawing, guessing) goes through its own "leave?" window; every other screen uses its back button
  const btn = document.querySelector("#" + cur + " [id$='-back']");
  if (btn && btn.offsetParent !== null) { btn.click(); return; }
  if (["search", "found", "onfin", "welcome", "res2", "result"].includes(cur)) {
    if (cur === "res2" || cur === "result") c.show("home");
    return;
  }
  c.show("home");
}
