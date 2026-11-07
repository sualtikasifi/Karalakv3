import { test } from "node:test";
import assert from "node:assert/strict";
import { capPerDevice, returnRate, planBroadcast, weekTopic } from "../lib/adminTasks.js";

test("a device's counters are held to what one phone can produce in a day", () => {
  assert.equal(capPerDevice("app__open__count", 5), 5);
  assert.equal(capPerDevice("app__open__count", 9_999_999), 2000);
  assert.equal(capPerDevice("rev__reward__micros", 90_000_000), 20_000_000);
  assert.equal(capPerDevice("app__open__count", -4), 0);
  assert.equal(capPerDevice("app__open__count", "12"), 0);
  assert.equal(capPerDevice("app__open__count", NaN), 0);
});

test("the return rate is the share of the earlier day that came back", () => {
  assert.equal(returnRate(new Set(["a", "b", "c", "d"]), new Set(["a", "b", "z"])), 0.5);
  assert.equal(returnRate(new Set(["a"]), new Set()), 0);
  assert.equal(returnRate(new Set(), new Set(["a"])), null);
});

test("the week topic is the ISO week, named the way the app subscribes", () => {
  assert.equal(weekTopic(new Date("2026-11-07T10:00:00Z")), "seen_2026w45");
  // the first days of January can belong to the last week of the year before
  assert.equal(weekTopic(new Date("2027-01-01T10:00:00Z")), "seen_2026w53");
  assert.equal(weekTopic(new Date("2026-01-05T00:00:00Z")), "seen_2026w02");
});

test("a plain broadcast is still the one send to the news topic", () => {
  const sends = planBroadcast({ title: "Merhaba", body: "Yeni etkinlik başladı" }, new Date("2026-11-07T10:00:00Z"));
  assert.deepEqual(sends.map((s) => [s.topic, s.condition, s.title]), [["news", undefined, "Merhaba"]]);
});

test("each language with a text gets its own send, and the rest get the Turkish one", () => {
  const sends = planBroadcast(
    { title: "Merhaba", body: "Yeni etkinlik", texts: { en: { title: "Hello", body: "New event" } }, fallback: true },
    new Date("2026-11-07T10:00:00Z")
  );
  const conds = sends.map((s) => s.condition);
  assert.deepEqual(conds, [
    "'news' in topics && 'news_tr' in topics",
    "'news' in topics && 'news_en' in topics",
    "'news' in topics && !('news_tr' in topics) && !('news_en' in topics)",
  ]);
  assert.equal(sends[1].title, "Hello");
  assert.equal(sends[2].title, "Merhaba");
});

test("without the fallback only the languages that have a text are sent to", () => {
  const sends = planBroadcast(
    { title: "Merhaba", body: "Yeni etkinlik", texts: { pt: { title: "Olá", body: "Novo evento" } }, fallback: false },
    new Date("2026-11-07T10:00:00Z")
  );
  assert.deepEqual(sends.map((s) => s.label), ["tr", "pt"]);
});

test("the lapsed audience is limited to devices not opened this week or last, in five topics at most", () => {
  const sends = planBroadcast(
    { title: "Seni özledik", body: "Gel", audience: "lapsed", texts: { en: { title: "We miss you", body: "Come" } } },
    new Date("2026-11-07T10:00:00Z")
  );
  assert.equal(sends.length, 2);
  for (const s of sends) {
    assert.match(s.condition, /'seen_v1' in topics/);
    assert.match(s.condition, /!\('seen_2026w45' in topics\)/);
    assert.match(s.condition, /!\('seen_2026w44' in topics\)/);
    assert.ok((s.condition.match(/ in topics/g) ?? []).length <= 5);
  }
});

test("no condition holds more than five topics, however many languages have a text", () => {
  const texts = Object.fromEntries(["tr", "en", "pt", "es"].map((l) => [l, { title: l, body: l }]));
  for (const s of planBroadcast({ title: "a", body: "b", texts, fallback: true }, new Date("2026-11-07T00:00:00Z"))) {
    assert.ok((s.condition.match(/ in topics/g) ?? []).length <= 5, s.condition);
  }
});

test("a broadcast with no text plans nothing", () => {
  assert.deepEqual(planBroadcast({ title: "", body: "" }, new Date()), []);
});
