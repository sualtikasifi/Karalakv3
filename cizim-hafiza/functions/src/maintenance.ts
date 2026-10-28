/**
 * One-off and scheduled maintenance that needs admin credentials but no Cloud Functions runtime — run from
 * functions/src/cli.ts on a GitHub Actions runner, like the league tasks in index.ts.
 */
import { logger } from "firebase-functions";
import * as admin from "firebase-admin";

/** The address domain browser accounts are made on now (see hosting/oyna/firebase.js). */
export const WEB_ACCOUNT_DOMAIN = "@users.karalak.com";
/** The domain they were made on before: a domain this project does not own, so reset mail for it could be read by anyone who registers it. */
export const OLD_WEB_ACCOUNT_DOMAIN = "@users.karalak.app";

/** The address an old browser account moves to, or null when [email] is not one. */
export function migratedWebEmail(email: string | undefined | null): string | null {
  if (!email || !email.endsWith(OLD_WEB_ACCOUNT_DOMAIN)) return null;
  const name = email.slice(0, -OLD_WEB_ACCOUNT_DOMAIN.length);
  return name ? name + WEB_ACCOUNT_DOMAIN : null;
}

/**
 * Moves every browser account still on the old address domain to the new one. The uid, password and progress stay
 * as they are; only the address changes, so the player signs in with the same user name and password as before.
 * Safe to run again: an account already moved no longer matches.
 */
export async function runMigrateWebEmails(): Promise<void> {
  const auth = admin.auth();
  let moved = 0;
  let conflicts = 0;
  let pageToken: string | undefined;
  do {
    const page = await auth.listUsers(1000, pageToken);
    for (const user of page.users) {
      const target = migratedWebEmail(user.email);
      if (!target) continue;
      try {
        await auth.updateUser(user.uid, { email: target });
        moved++;
      } catch (e) {
        // The new address is already taken: someone registered the same user name after the move. Left as it is and
        // reported, so it can be sorted out by hand; the old account still signs in through the web's fallback.
        conflicts++;
        logger.warn(`Web account ${user.uid} (${user.email}) not moved:`, e);
      }
    }
    pageToken = page.pageToken;
  } while (pageToken);
  logger.info(`Web accounts: moved ${moved}, ${conflicts} conflict(s)`);
}

/** Deletes every document [query] matches, in batches the size Firestore allows. */
async function deleteAll(query: FirebaseFirestore.Query): Promise<number> {
  let deleted = 0;
  for (;;) {
    const page = await query.limit(400).get();
    if (page.empty) return deleted;
    const batch = query.firestore.batch();
    page.docs.forEach((d) => batch.delete(d.ref));
    await batch.commit();
    deleted += page.size;
    if (page.size < 400) return deleted;
  }
}

/** [rows] without the ones belonging to [uid]; the same array when nothing was removed. */
export function withoutUid<T extends { uid?: string | null }>(rows: T[] | undefined, uid: string): T[] | undefined {
  if (!rows) return rows;
  const kept = rows.filter((r) => r.uid !== uid);
  return kept.length === rows.length ? rows : kept;
}

/**
 * Everything an account left behind, anywhere in the database: its profile and every subcollection under it (the
 * cloud save included), its names and friend code, its entry in each friend's list, its recorded and queued rounds,
 * daily attempts, reports by and about it, duels, moderation records, requests and invites it left in other inboxes,
 * its statistics rows and coupon redemptions, its referral ledger, and its rows in the published league table.
 * Rooms are not touched: every room is removed within a day of its last activity anyway (runCleanupAbandonedRooms).
 */
export async function deleteAccountData(db: FirebaseFirestore.Firestore, uid: string): Promise<void> {
  const userRef = db.doc(`users/${uid}`);
  for (const friend of (await userRef.collection("friends").get()).docs) {
    await db.doc(`users/${friend.id}/friends/${uid}`).delete();
  }
  await deleteAll(db.collection("friendCodes").where("uid", "==", uid));
  await deleteAll(db.collection("usernames").where("uid", "==", uid));
  await db.doc(`usernameOwners/${uid}`).delete();
  for (const [runs, items] of [["ghostRuns", "ghostRunItems"], ["pendingRuns", "pendingRunItems"]]) {
    for (const run of (await db.collection(runs).where("uid", "==", uid).get()).docs) {
      await db.doc(`${items}/${run.id}`).delete();
      await run.ref.delete();
    }
    await deleteAll(db.collection(items).where("uid", "==", uid));
  }
  for (const collection of ["pendingDailyChallenges", "bugReports", "detectorEvents", "penalties"]) {
    await deleteAll(db.collection(collection).where("uid", "==", uid));
  }
  for (const [collection, field] of [
    ["drawingReports", "reporterUid"], ["drawingReports", "reportedUid"],
    ["playerReports", "reporterUid"], ["playerReports", "reportedUid"],
    ["duels", "challengerUid"], ["duels", "opponentUid"],
  ]) {
    await deleteAll(db.collection(collection).where(field, "==", uid));
  }
  await db.doc(`moderationStrikes/${uid}`).delete();
  // Left in other players' inboxes. Only requests sent since fromUid was added can be found; older ones carry
  // nothing but a nickname and disappear when answered.
  await deleteAll(db.collectionGroup("friendRequests").where("fromUid", "==", uid));
  await deleteAll(db.collectionGroup("invites").where("fromUid", "==", uid));
  await deleteAll(db.collectionGroup("devices").where("uid", "==", uid));
  await deleteAll(db.collectionGroup("users").where("uid", "==", uid));
  for (const coupon of await db.collection("coupons").listDocuments()) {
    await coupon.collection("redemptions").doc(uid).delete();
  }
  await db.doc(`referrals/${uid}`).delete();
  await db.doc(`referralStats/${uid}`).delete();
  // The published league table (and the last month's winners) name accounts by uid and nickname.
  const table = await db.doc("leaderboards/global").get();
  if (table.exists) {
    const entries = table.get("entries") as { uid?: string | null }[] | undefined;
    const humans = table.get("humans") as { uid?: string | null }[] | undefined;
    const ended = table.get("endedPeriod") as { entries?: { uid?: string | null }[] } | undefined;
    const last = table.get("lastPeriod") as { winners?: { uid?: string | null }[] } | undefined;
    const update: Record<string, unknown> = {};
    if (withoutUid(entries, uid) !== entries) update.entries = withoutUid(entries, uid);
    if (withoutUid(humans, uid) !== humans) update.humans = withoutUid(humans, uid);
    if (ended && withoutUid(ended.entries, uid) !== ended.entries) update["endedPeriod.entries"] = withoutUid(ended.entries, uid);
    if (last && withoutUid(last.winners, uid) !== last.winners) update["lastPeriod.winners"] = withoutUid(last.winners, uid);
    if (Object.keys(update).length > 0) await table.ref.update(update);
  }
  await db.recursiveDelete(userRef);
}

/**
 * Works through the accounts that asked to be deleted (deletionRequests/{uid}, filed by the app or the web right
 * before they sign out): removes their data and their sign-in, then the request itself. A request that fails stays
 * and is tried again on the next run.
 */
export async function runProcessDeletions(): Promise<void> {
  const db = admin.firestore();
  const requests = await db.collection("deletionRequests").limit(100).get();
  let done = 0;
  for (const request of requests.docs) {
    const uid = request.id;
    try {
      await deleteAccountData(db, uid);
      await admin.auth().deleteUser(uid).catch((e: { code?: string }) => {
        if (e.code !== "auth/user-not-found") throw e;
      });
      await request.ref.delete();
      done++;
    } catch (e) {
      logger.error(`Deletion of ${uid} not finished, will retry:`, e);
    }
  }
  logger.info(`Deletions: ${done} of ${requests.size} account(s) removed`);
}
