// Firebase layer. Loaded lazily so the game still works offline / before config is filled in.
import { firebaseConfig, firebaseReady } from "./config.js";

const V = "10.14.1";
const CDN = `https://www.gstatic.com/firebasejs/${V}`;
const RUN_WORD_COUNT = 10, SHARD_COUNT = 1000, BAND_SIZE = 10, MIN_CORRECT = 2, MAX_BAND = 9, CANDIDATES = 5;
let api = null;

async function init() {
  if (api) return api;
  if (!firebaseReady()) throw new Error("no-config");
  const [app, auth, fs] = await Promise.all([
    import(`${CDN}/firebase-app.js`), import(`${CDN}/firebase-auth.js`), import(`${CDN}/firebase-firestore.js`),
  ]);
  const a = app.initializeApp(firebaseConfig);
  const au = auth.getAuth(a);
  if (!au.currentUser) await auth.signInAnonymously(au);
  await au.authStateReady?.();
  api = { fs, db: fs.getFirestore(a), auth: au };
  return api;
}

export const levelBandFor = (level) => Math.floor((Math.max(1, level) - 1) / BAND_SIZE);

// Mirrors GhostRunRepositoryImpl.findOpponent (without the bot fallback): own band first, then outwards.
export async function findOpponent(language, level = 1) {
  const { fs, db, auth } = await init();
  const own = levelBandFor(level), uid = auth.currentUser.uid;
  const bands = Array.from({ length: MAX_BAND + 1 }, (_, i) => i).sort((a, b) => Math.abs(a - own) - Math.abs(b - own) || a - b);
  const run = async (band, pivot, above) => {
    const q = fs.query(fs.collection(db, "ghostRuns"),
      fs.where("shard", above ? ">=" : "<", pivot), fs.where("language", "==", language),
      fs.where("mode", "==", "NORMAL"), fs.where("wordCount", "==", RUN_WORD_COUNT),
      fs.where("levelBand", "==", band), fs.orderBy("shard", above ? "asc" : "desc"), fs.limit(CANDIDATES));
    const snap = await fs.getDocs(q);
    return snap.docs.map((d) => ({ id: d.id, ...d.data() })).find((r) => r.uid !== uid && !("itemsJson" in r));
  };
  for (const band of bands) {
    const pivot = Math.floor(Math.random() * SHARD_COUNT);
    const found = (await run(band, pivot, true)) || (await run(band, pivot, false));
    if (found) return found;
  }
  return null;
}

export async function loadItems(runId) {
  const { fs, db } = await init();
  const snap = await fs.getDoc(fs.doc(db, "ghostRunItems", runId));
  return snap.exists() ? JSON.parse(snap.data().itemsJson) : [];
}

// Files a finished round in the review queue, same document shape as Android's write().
export async function recordRound({ language, nickname, wordIds, perWord, items }) {
  const correctCount = perWord.filter((p) => p.isCorrect).length;
  if (wordIds.length < RUN_WORD_COUNT || correctCount < MIN_CORRECT) return false;
  const { fs, db, auth } = await init();
  const uid = auth.currentUser.uid;
  const ref = fs.doc(fs.collection(db, "pendingRuns"));
  const correctTimes = perWord.filter((p) => p.isCorrect).map((p) => p.responseTimeMs);
  const batch = fs.writeBatch(db);
  batch.set(ref, {
    uid, nickname, level: 1, frameId: "SCRIBBLER", levelBand: 0, shard: Math.floor(Math.random() * SHARD_COUNT),
    language, mode: "NORMAL", wordCount: wordIds.length, wordIds,
    totalScore: perWord.reduce((s, p) => s + p.pointsAwarded, 0), correctCount,
    fastestCorrectMs: correctTimes.length ? Math.min(...correctTimes) : null,
    perWord, xpEarned: 0, createdAt: Date.now(),
  });
  batch.set(fs.doc(db, "pendingRunItems", ref.id), { uid, itemsJson: JSON.stringify(items) });
  await batch.commit();
  return true;
}
