// Turns web-tests/shots into one page (shots/index.html): every screen of every iPhone size side by side, to look at on any phone or computer.
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
const dir = path.join(path.dirname(fileURLToPath(import.meta.url)), "shots");
const all = fs.readdirSync(dir).filter((f) => f.endsWith(".jpg")).sort(), flows = all.filter((f) => f.startsWith("flow_")), files = all.filter((f) => !f.startsWith("flow_"));
const diffs = fs.existsSync(path.join(dir, "diffs.json")) ? JSON.parse(fs.readFileSync(path.join(dir, "diffs.json"), "utf8")) : [];
const devices = [...new Set(files.map((f) => f.replace(/_(home|store|settings|ach|league|runes|friends|account|report|onl|offs|duels|welcome|draw|guess)\.jpg$/, "")))];
const screens = [...new Set(files.map((f) => f.match(/_([a-z]+)\.jpg$/)[1]))];
const when = new Date().toISOString().replace("T", " ").slice(0, 16) + " UTC";
const html = `<!doctype html><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>iPhone ekranları</title>
<style>body{font:14px system-ui;margin:0;background:#2a1a10;color:#fff3d9}h1{font-size:18px;margin:12px}small{opacity:.7;margin-left:12px}
.row{display:flex;gap:10px;overflow-x:auto;padding:8px 12px}.c{flex:none;text-align:center}.c img{display:block;height:520px;border-radius:12px;border:2px solid #5a3a22}
h2{font-size:16px;margin:14px 12px 0}</style>
<h1>iPhone (WebKit) ekranları <small>${when}</small></h1>
<h2>Safari ile Chrome arasındaki fark (farklı piksel oranı; yüksek olan Safari'ye özel olabilir)</h2><div style="padding:8px 12px">${[...diffs].sort((a, b) => b.pct - a.pct).slice(0, 14).map((d) => `${d.name} / ${d.id}: <b>${d.pct}%</b> <a style="color:#ffd25a" href="${d.name.replace(/\W+/g, "_")}_${d.id}_diff.png">fark resmi</a>`).join("<br>")}</div>
<h2>Oyun akışları (öğretici, hızlı eşleş, günlük)</h2><div class="row">${flows.map((f) => `<div class="c"><img loading="lazy" src="${f}"><div>${f.replace(/^flow_|\.jpg$/g, "").replace(/_/g, " ")}</div></div>`).join("")}</div>
${screens.map((sc) => `<h2>${sc}</h2><div class="row">${devices.map((d) => files.includes(`${d}_${sc}.jpg`) ? `<div class="c"><img loading="lazy" src="${d}_${sc}.jpg"><div>${d.replace(/_/g, " ")}</div></div>` : "").join("")}</div>`).join("\n")}`;
fs.writeFileSync(path.join(dir, "index.html"), html);
console.log(`gallery: ${files.length} screenshots, ${screens.length} screens`);
