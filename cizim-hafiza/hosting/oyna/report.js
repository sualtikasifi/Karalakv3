import * as net from "./firebase.js";
const T = { tr: { title: "Sorun Bildir", introT: "Bir sorun mu yaşıyorsun?", intro: "Karşılaştığın bir hatayı, önerini veya sorunu bizimle paylaşabilirsin. En kısa sürede talebini inceleyeceğiz.", type: "Bildirim Türü", sug: "Öneri", com: "Şikayet", msg: "Mesajın", ph: "Ne düşündüğünü ya da karşılaştığın sorunu buraya yaz…", send: "Gönder", sending: "Gönderiliyor…", ok: "Teşekkürler, bildirimin bize ulaştı!", err: "Gönderilemedi, tekrar dene.", hist: "Geçmiş Bildirimlerim", unseen: "Henüz görülmedi", seen: "Görüldü", del: "Sil", delAll: "Tümünü Sil", delQ: "Bu bildirim kalıcı olarak silinecek.", yes: "Sil", no: "Vazgeç" },
  en: { title: "Report a Bug", introT: "Having a problem?", intro: "Share a bug, a suggestion or a problem with us. We'll look at it as soon as we can.", type: "Report type", sug: "Suggestion", com: "Complaint", msg: "Your message", ph: "Write what you think or the problem you ran into…", send: "Send", sending: "Sending…", ok: "Thanks, your report reached us!", err: "Couldn't send, try again.", hist: "My past reports", unseen: "Not seen yet", seen: "Seen", del: "Delete", delAll: "Delete all", delQ: "This report will be deleted for good.", yes: "Delete", no: "Cancel" } };
let cat = "SUGGESTION", list = [], note = "", busy = false;
const el = (tag, cls, html) => { const e = document.createElement(tag); if (cls) e.className = cls; if (html != null) e.innerHTML = html; return e; };
const esc = (s) => String(s).replace(/[&<>"]/g, (ch) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" }[ch]));
export function layoutReport(c) { const { place, $ } = c; place($("rp-back"), 20, 40, 110, 106); place($("rp-sign"), 150, 80, 541, 240); place($("rp-title"), 200, 200, 440, 100); const l = $("rp-body"); l.style.left = 30 * c.u() + "px"; l.style.top = 340 * c.u() + "px"; l.style.width = 781 * c.u() + "px"; l.style.height = c.stageH() - 340 * c.u() + "px"; }
export async function renderReport(c, reload = true) {
  const { $, lang } = c, x = T[lang()]; $("rp-title").textContent = x.title;
  const body = $("rp-body"); body.replaceChildren();
  const card = el("div", "rp-card", `<b>${x.introT}</b><p>${x.intro}</p><div class="rp-l">${x.type}</div>`);
  const row = el("div", "rp-row");
  [["SUGGESTION", x.sug, "rp_pill_orange"], ["COMPLAINT", x.com, "rp_pill_blue"]].forEach(([k, t, img]) => { const b = el("button", "rp-pill" + (cat === k ? " on" : ""), t); b.style.backgroundImage = `url(img/${img}.webp)`; b.onclick = () => { cat = k; renderReport(c, false); }; row.append(b); });
  card.append(row, el("div", "rp-l", x.msg));
  const ta = el("textarea", "rp-ta"); ta.placeholder = x.ph; ta.maxLength = 2000; ta.value = body.dataset.draft || ""; ta.oninput = () => { body.dataset.draft = ta.value; cnt.textContent = ta.value.length + " / 2000"; };
  const cnt = el("div", "rp-cnt", (ta.value.length) + " / 2000");
  const send = el("button", "rp-send", busy ? x.sending : x.send);
  send.onclick = async () => { if (busy || !ta.value.trim()) return; busy = true; note = ""; send.textContent = x.sending; try { await net.sendReport(cat, ta.value); ta.value = ""; body.dataset.draft = ""; note = x.ok; list = await net.myReports().catch(() => list); } catch { note = x.err; } busy = false; renderReport(c, false); };
  card.append(ta, cnt, send, el("div", "rp-note", note)); body.append(card);
  if (reload) { try { list = await net.myReports(); } catch { list = []; } if (c.current() !== "report") return; return renderReport(c, false); }
  if (list.length) {
    body.append(el("div", "rp-h", x.hist));
    list.forEach((r) => { const it = el("div", "rp-item", `<div class="rp-t"><span>${r.category === "COMPLAINT" ? x.com : x.sug}</span><em class="${r.seenAtMillis ? "ok" : ""}">${r.seenAtMillis ? x.seen : x.unseen}</em></div><p>${esc(r.description)}</p>`);
      const d = el("button", "rp-del", "🗑 " + x.del); d.onclick = () => c.confirm(x.delQ, x.yes, async () => { await net.deleteReport(r.id).catch(() => {}); list = list.filter((q) => q.id !== r.id); renderReport(c, false); }); it.append(d); body.append(it); });
  }
}
