// Shared by the worker's jobs: a Google access token from the Firebase service account, and Firestore / FCM over REST.

const b64url = (buf) => btoa(String.fromCharCode(...new Uint8Array(buf))).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
const enc = (o) => b64url(new TextEncoder().encode(JSON.stringify(o)));

export async function accessToken(sa) {
  const now = Math.floor(Date.now() / 1000);
  const head = enc({ alg: "RS256", typ: "JWT" });
  const body = enc({
    iss: sa.client_email, scope: "https://www.googleapis.com/auth/cloud-platform",
    aud: "https://oauth2.googleapis.com/token", iat: now, exp: now + 3600,
  });
  const der = Uint8Array.from(atob(sa.private_key.replace(/-----[^-]+-----/g, "").replace(/\s+/g, "")), (c) => c.charCodeAt(0));
  const key = await crypto.subtle.importKey("pkcs8", der, { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" }, false, ["sign"]);
  const sig = await crypto.subtle.sign("RSASSA-PKCS1-v1_5", key, new TextEncoder().encode(`${head}.${body}`));
  const res = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "content-type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({ grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer", assertion: `${head}.${body}.${b64url(sig)}` }),
  });
  if (!res.ok) throw new Error(`token ${res.status}: ${await res.text()}`);
  return (await res.json()).access_token;
}

/** Firestore REST value → plain JS value. */
export function decode(v) {
  if (!v) return undefined;
  if ("stringValue" in v) return v.stringValue;
  if ("integerValue" in v) return Number(v.integerValue);
  if ("doubleValue" in v) return v.doubleValue;
  if ("booleanValue" in v) return v.booleanValue;
  if ("timestampValue" in v) return Date.parse(v.timestampValue);
  if ("nullValue" in v) return null;
  if ("mapValue" in v) return Object.fromEntries(Object.entries(v.mapValue.fields ?? {}).map(([k, x]) => [k, decode(x)]));
  if ("arrayValue" in v) return (v.arrayValue.values ?? []).map(decode);
  return undefined;
}

/** Plain JS value → Firestore REST value (strings, numbers, booleans, null, arrays, plain objects). */
export function encode(x) {
  if (x === null || x === undefined) return { nullValue: null };
  if (typeof x === "string") return { stringValue: x };
  if (typeof x === "boolean") return { booleanValue: x };
  if (typeof x === "number") return Number.isInteger(x) ? { integerValue: String(x) } : { doubleValue: x };
  if (Array.isArray(x)) return { arrayValue: { values: x.map(encode) } };
  return { mapValue: { fields: Object.fromEntries(Object.entries(x).map(([k, v]) => [k, encode(v)])) } };
}

/** A signed-in session on the project's Firestore and FCM. */
export async function session(env) {
  const sa = JSON.parse(env.FIREBASE_SERVICE_ACCOUNT);
  const project = sa.project_id;
  const auth = { authorization: `Bearer ${await accessToken(sa)}`, "content-type": "application/json" };
  const root = `projects/${project}/databases/(default)/documents`;
  const api = `https://firestore.googleapis.com/v1/${root}`;
  return {
    project, auth, api, root,
    /** Runs a structured query; every document comes back decoded, with its id. */
    async query(structuredQuery) {
      const res = await fetch(`${api}:runQuery`, { method: "POST", headers: auth, body: JSON.stringify({ structuredQuery }) });
      if (!res.ok) throw new Error(`query ${structuredQuery.from?.[0]?.collectionId} ${res.status}: ${await res.text()}`);
      return (await res.json()).filter((r) => r.document).map((r) => ({
        id: r.document.name.split("/").pop(),
        data: Object.fromEntries(Object.entries(r.document.fields ?? {}).map(([k, v]) => [k, decode(v)])),
      }));
    },
    /** One document's plain fields, or null when it does not exist. */
    async get(path) {
      const res = await fetch(`${api}/${path}`, { headers: auth });
      if (res.status === 404) return null;
      if (!res.ok) throw new Error(`get ${path} ${res.status}: ${await res.text()}`);
      const d = await res.json();
      return Object.fromEntries(Object.entries(d.fields ?? {}).map(([k, v]) => [k, decode(v)]));
    },
    /** Sets only the given fields of a document (creating it when missing). */
    async patch(path, fields) {
      const mask = Object.keys(fields).map((f) => `updateMask.fieldPaths=${encodeURIComponent(f)}`).join("&");
      const res = await fetch(`${api}/${path}?${mask}`, {
        method: "PATCH", headers: auth,
        body: JSON.stringify({ fields: Object.fromEntries(Object.entries(fields).map(([k, v]) => [k, v && v.__ts ? { timestampValue: new Date(v.__ts).toISOString() } : encode(v)])) }),
      });
      if (!res.ok) throw new Error(`patch ${path} ${res.status}: ${await res.text()}`);
    },
    /** Replaces a whole document. */
    async put(path, fields) {
      const res = await fetch(`${api}/${path}`, {
        method: "PATCH", headers: auth,
        body: JSON.stringify({ fields: Object.fromEntries(Object.entries(fields).map(([k, v]) => [k, v && v.__ts ? { timestampValue: new Date(v.__ts).toISOString() } : encode(v)])) }),
      });
      if (!res.ok) throw new Error(`put ${path} ${res.status}: ${await res.text()}`);
    },
    /** arrayUnion on one field, creating the document when it is missing. */
    async arrayUnion(path, field, values) {
      const res = await fetch(`${api}:commit`, {
        method: "POST", headers: auth,
        body: JSON.stringify({ writes: [{
          update: { name: `${root}/${path}`, fields: {} },
          updateMask: { fieldPaths: [] },
          updateTransforms: [{ fieldPath: field, appendMissingElements: { values: values.map(encode) } }],
        }] }),
      });
      if (!res.ok) throw new Error(`arrayUnion ${path} ${res.status}: ${await res.text()}`);
    },
    /** One FCM message (a token, topic or condition inside). Returns the response. */
    async send(message) {
      return fetch(`https://fcm.googleapis.com/v1/projects/${project}/messages:send`, {
        method: "POST", headers: auth, body: JSON.stringify({ message }),
      });
    },
  };
}
