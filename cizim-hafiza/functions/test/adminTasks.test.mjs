import { test } from "node:test";
import assert from "node:assert/strict";
import { capPerDevice, returnRate } from "../lib/adminTasks.js";

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
