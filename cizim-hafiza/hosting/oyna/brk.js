// The pause between drawing and guessing (BreakScreen.kt): the mascot over a gold dial counting 3-2-1 under the hanging sign.
import { S } from "./i18n.js";
const TOTAL = 3, R = 186, CIRC = 2 * Math.PI * R;
let timer = 0, raf = 0;
export function layoutBreak(c) {
  const { place, $ } = c;
  place($("bk-l1"), 186, 264, 474, 66); place($("bk-l2"), 186, 330, 474, 66); place($("bk-sub"), 256, 486, 344, 80);
  place($("bk-dial"), 422 - 300, 1170 - 300, 600, 600);
}
export function startBreak(c, done) {
  clearInterval(timer); cancelAnimationFrame(raf);
  const { $ } = c; $("bk-l1").textContent = S("get_ready_line1"); $("bk-l2").textContent = S("get_ready_line2"); $("bk-sub").textContent = S("get_ready_sub");
  c.show("brk");
  const arc = $("bk-arc"), num = $("bk-num"); let left = TOTAL, t0 = performance.now();
  const paint = () => {
    num.textContent = left; num.classList.remove("pop"); void num.offsetWidth; num.classList.add("pop");
    arc.style.transition = "none"; arc.style.strokeDashoffset = CIRC * (1 - left / TOTAL); void arc.getBoundingClientRect();
    arc.style.transition = "stroke-dashoffset 1s linear"; arc.style.strokeDashoffset = CIRC * (1 - Math.max(0, left - 1) / TOTAL);
    c.sfx && left < TOTAL && c.sfx.tick && c.sfx.tick();
  };
  paint();
  timer = setInterval(() => { if (c.current() !== "brk") return clearInterval(timer); left--; if (left <= 0) { clearInterval(timer); done(); } else paint(); }, 1000);
}
export function stopBreak() { clearInterval(timer); }
