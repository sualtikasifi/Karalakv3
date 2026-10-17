// Minimal in-memory Firebase for browser tests (app, auth, firestore).

const store = window.__store = window.__store || {};
const subs = [];
const clone = (x) => x === undefined ? undefined : JSON.parse(JSON.stringify(x));
const SERVER = { __ts: 1 }, DEL = { __del: 1 };
const key = (p) => p.join("/");
function setPath(obj, path, v) { const k = path.split("."); let o = obj; for (let i = 0; i < k.length - 1; i++) o = o[k[i]] = o[k[i]] || {}; if (v && v.__del) delete o[k[k.length - 1]]; else o[k[k.length - 1]] = resolve(v); }
const resolve = (v) => v && v.__ts ? Date.now() : v && v.__union ? v.vals : v;
function notify() { subs.forEach((f) => f()); } window.__notify = notify;
const snap = (p) => ({ id: p[p.length - 1], exists: () => key(p) in store, data: () => clone(store[key(p)]) });
export const initializeApp = () => ({});
export const getAuth = () => { const a = window.__auth = window.__auth || { currentUser: null, authStateReady: async () => {} }; return a; };
export const signInAnonymously = async (a) => { a.currentUser = { uid: window.__uid || "me", isAnonymous: true, displayName: "", email: "", photoURL: "" }; };
export class GoogleAuthProvider {}
export const linkWithPopup = async () => {}; export const signInWithPopup = async () => {}; export const signOut = async () => {};
export const getFirestore = () => ({});
export const doc = (db, ...p) => { if (p.length === 1 && p[0].path) return p[0]; return { path: p.join("/").split("/"), kind: "doc" }; };
export const collection = (db, ...p) => ({ path: p.join("/").split("/"), kind: "col" });
export const serverTimestamp = () => SERVER; export const deleteField = () => DEL; export const arrayUnion = (...v) => ({ __union: 1, vals: v });
export const getDoc = async (r) => snap(r.path);
export const setDoc = async (r, d, o) => { const k = key(r.path); store[k] = o && o.merge ? { ...(store[k] || {}), ...clone(d) } : clone(d); for (const [kk, v] of Object.entries(store[k])) if (v && v.__ts) store[k][kk] = Date.now(); notify(); };
export const updateDoc = async (r, d) => { const k = key(r.path); if (!(k in store)) throw new Error("missing"); for (const [p, v] of Object.entries(d)) { if (v && v.__union) { const cur = store[k][p] || []; store[k][p] = [...new Set([...cur, ...v.vals])]; } else setPath(store[k], p, v); } notify(); };
export const deleteDoc = async (r) => { delete store[key(r.path)]; notify(); };
export const addDoc = async (c, d) => { const id = "a" + Math.random().toString(36).slice(2, 8); store[key([...c.path, id])] = clone(d); notify(); return { id }; };
export const writeBatch = () => { const ops = []; return { set: (r, d) => ops.push(() => setDoc(r, d)), delete: (r) => ops.push(() => deleteDoc(r)), update: (r, d) => ops.push(() => updateDoc(r, d)), commit: async () => { for (const o of ops) await o(); } }; };
export const runTransaction = async (db, fn) => { const ops = []; const tx = { get: async (r) => snap(r.path), set: (r, d) => ops.push(() => setDoc(r, d)), update: (r, d) => ops.push(() => updateDoc(r, d)) }; const res = await fn(tx); for (const o of ops) await o(); return res; };
export const onSnapshot = (r, cb) => {
  const emit = () => { if (r.kind === "doc") cb(snap(r.path)); else { const pre = key(r.path) + "/"; const docs = Object.keys(store).filter((k) => k.startsWith(pre) && k.slice(pre.length).indexOf("/") < 0).map((k) => snap(k.split("/"))); cb({ docs }); } };
  subs.push(emit); emit(); return () => { const i = subs.indexOf(emit); if (i >= 0) subs.splice(i, 1); };
};
export const query = (c) => c; export const where = () => null; export const orderBy = () => null; export const limit = () => null; export const limitToLast = () => null;
export const getDocs = async (c) => { const pre = key(c.path) + "/"; return { docs: Object.keys(store).filter((k) => k.startsWith(pre) && k.slice(pre.length).indexOf("/") < 0).map((k) => snap(k.split("/"))) }; };
