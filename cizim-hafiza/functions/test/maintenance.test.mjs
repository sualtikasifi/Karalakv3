import test from "node:test";
import assert from "node:assert/strict";
import { migratedWebEmail } from "../lib/maintenance.js";

test("an old browser account moves to the owned domain, keeping its user name", () => {
  assert.equal(migratedWebEmail("ahmet_34@users.karalak.app"), "ahmet_34@users.karalak.com");
});

test("anything else is left alone", () => {
  assert.equal(migratedWebEmail("ahmet@users.karalak.com"), null);
  assert.equal(migratedWebEmail("someone@gmail.com"), null);
  assert.equal(migratedWebEmail("@users.karalak.app"), null);
  assert.equal(migratedWebEmail(undefined), null);
});

import { withoutUid } from "../lib/maintenance.js";

test("a deleted account's rows leave the published table", () => {
  const rows = [{ uid: "a", nickname: "A" }, { uid: "b", nickname: "B" }, { uid: null, nickname: "bot" }];
  assert.deepEqual(withoutUid(rows, "a"), [{ uid: "b", nickname: "B" }, { uid: null, nickname: "bot" }]);
});

test("an untouched table is handed back as it is (nothing to write)", () => {
  const rows = [{ uid: "b" }];
  assert.equal(withoutUid(rows, "a"), rows);
  assert.equal(withoutUid(undefined, "a"), undefined);
});
