import { onDocumentCreated, onDocumentWritten } from "firebase-functions/v2/firestore";
import { onSchedule } from "firebase-functions/v2/scheduler";
import { logger } from "firebase-functions";
import * as admin from "firebase-admin";

admin.initializeApp();

/**
 * Fires whenever a friend match invite is written to
 * users/{uid}/invites/{inviteId} (see FriendRepositoryImpl.sendMatchInvite
 * in the Android app) and pushes a notification to the recipient's device —
 * this is what lets an invite reach someone whose app isn't currently open
 * (FriendInviteMessagingService.kt only fires from a live app process; this
 * function is what wakes it up).
 *
 * The message payload is data-only (no top-level `notification` field) on
 * purpose: a `notification` payload gets displayed directly by the OS when
 * the app is backgrounded/killed, bypassing FriendInviteMessagingService
 * entirely — which means no custom channel, no localized text (this
 * function has no idea what language the recipient's device is in), and no
 * tap-through PendingIntent to open the app. A data-only payload always
 * hands control to onMessageReceived, which builds everything from the
 * recipient's own local string resources.
 */
export const onInviteCreated = onDocumentCreated(
  "users/{uid}/invites/{inviteId}",
  async (event) => {
    const invite = event.data?.data();
    if (!invite) return;

    const { uid, inviteId } = event.params;
    const fromNickname: string | undefined = invite.fromNickname;
    const roomCode: string | undefined = invite.roomCode;
    if (!fromNickname || !roomCode) {
      logger.warn(`Invite ${inviteId} missing fromNickname/roomCode, skipping push`);
      return;
    }

    // users/{uid}/private/device, not users/{uid} itself: the parent profile
    // document is readable by any signed-in player (that is how a friend list
    // resolves nicknames), so the push token — which identifies a specific
    // physical device — is kept in the owner-only private/ subcollection.
    // This function runs with admin credentials and bypasses rules, so the
    // move costs delivery nothing. Falls back to the old location so a device
    // that has not opened the app since the move still receives invites.
    const db = admin.firestore();
    const privateDoc = await db
      .collection("users").doc(uid)
      .collection("private").doc("device")
      .get();
    let fcmToken: string | undefined = privateDoc.get("fcmToken");
    if (!fcmToken) {
      const legacyDoc = await db.collection("users").doc(uid).get();
      fcmToken = legacyDoc.get("fcmToken");
    }
    if (!fcmToken) {
      // Recipient has never opened notifications on this device (or is on
      // an old install from before push was added) — not an error, they'll
      // still see the invite via the in-app banner next time they open the
      // app, same as before this feature existed.
      return;
    }

    try {
      await admin.messaging().send({
        token: fcmToken,
        data: {
          inviteId,
          roomCode,
          fromNickname,
        },
        android: {
          priority: "high",
        },
      });
    } catch (error) {
      // A stale/uninstalled-app token is expected over time, not a bug —
      // just log it rather than retrying (the invite itself already exists
      // in Firestore regardless, so nothing is lost).
      logger.warn(`Failed to send invite push for ${inviteId}:`, error);
    }
  }
);

/**
 * Clamps an impossible score the moment it is written.
 *
 * Scores are computed on the player's own device and written straight into
 * `rooms/{code}.players.{uid}.totalScore`. firestore.rules can stop a player
 * rewriting *someone else's* row, but it cannot check arithmetic — nothing
 * there prevents a modified client from claiming 9999 for its own. This is
 * the arithmetic check: the maximum a round can possibly be worth is one
 * correct answer per word, each with the speed bonus, so anything above that
 * ceiling is rejected and pulled back down to it.
 *
 * Deliberately a clamp rather than a ban. The honest failure modes here (an
 * older client, a rounding difference, a rule tweaked in the app but not
 * here) should degrade to "your score was capped", never to a locked
 * account — and a cheater capped to the same ceiling as everyone else has
 * nothing left to gain.
 *
 * Keep POINTS_CORRECT / SPEED_BONUS_POINTS in sync with GameConstants.kt.
 */
const POINTS_CORRECT = 5;
const SPEED_BONUS_POINTS = 2;
const MAX_POINTS_PER_WORD = POINTS_CORRECT + SPEED_BONUS_POINTS;

export const clampImpossibleScores = onDocumentWritten(
  "rooms/{roomCode}",
  async (event) => {
    const after = event.data?.after;
    if (!after?.exists) return;

    const players = (after.get("players") ?? {}) as Record<string, Record<string, unknown>>;
    const wordIds = (after.get("wordIds") ?? []) as unknown[];
    // Before a round starts there is no word list to bound the score by, and
    // the scores are all zero anyway.
    if (wordIds.length === 0) return;

    const ceiling = wordIds.length * MAX_POINTS_PER_WORD;
    const corrections: Record<string, number> = {};

    for (const [uid, data] of Object.entries(players)) {
      const claimed = typeof data.totalScore === "number" ? data.totalScore : 0;
      if (claimed > ceiling || claimed < 0) {
        corrections[`players.${uid}.totalScore`] = Math.min(Math.max(claimed, 0), ceiling);
        logger.warn(
          `Room ${event.params.roomCode}: ${uid} claimed ${claimed}, ceiling is ${ceiling} — clamping`
        );
      }
    }

    if (Object.keys(corrections).length > 0) {
      // This write re-triggers this same function; the second pass finds
      // every score already within the ceiling and writes nothing, so the
      // recursion terminates after exactly one extra invocation.
      await after.ref.update(corrections);
    }
  }
);

/**
 * Deletes abandoned rooms and their subcollections once a day.
 *
 * Nothing in the app ever removes a room: firestore.rules denies delete
 * outright, and a player leaving only flips a `left` flag. Every room ever
 * created — plus its `results` documents, which carry the full stroke data
 * for every drawing, and its `reactions` log — therefore accumulates
 * forever, and the Firestore bill grows with it for storage nobody can
 * reach any more.
 *
 * Room 130246 is the permanent bot room and is explicitly never collected.
 */
export async function runCleanupAbandonedRooms(): Promise<void> {
  const BOT_ROOM = "130246";
  const MAX_AGE_MS = 24 * 60 * 60 * 1000;
  const cutoff = Date.now() - MAX_AGE_MS;
  const db = admin.firestore();

  const rooms = await db.collection("rooms").get();
  let deleted = 0;

  for (const room of rooms.docs) {
    if (room.id === BOT_ROOM) continue;

    // startedAt is only set once a match begins, so fall back to
    // createdAt for a lobby nobody ever played in.
    const lastActivity =
      (room.get("startedAt") as number | undefined) ??
      (room.get("createdAt") as number | undefined) ??
      0;
    if (lastActivity > cutoff) continue;

    // recursiveDelete removes the document together with its results/ and
    // reactions/ subcollections, which a plain delete() would orphan.
    await db.recursiveDelete(room.ref);
    deleted++;
  }

  logger.info(`Cleanup: removed ${deleted} abandoned room(s) of ${rooms.size}`);
}

// Not currently deployed — see functions/DEPLOY.md. Cloud Functions require
// the Blaze plan regardless of how they're deployed, which this project is
// staying off of, so this schedule-based task instead runs as a plain script
// from a GitHub Actions cron (.github/workflows/league-scheduler.yml),
// calling runCleanupAbandonedRooms() directly with no Cloud Functions
// runtime involved. Kept here, wired up and ready, for the day this project
// does go Blaze — at which point delete the cron workflow and deploy this.
export const cleanupAbandonedRooms = onSchedule(
  { schedule: "every day 04:00", timeZone: "Europe/Istanbul" },
  runCleanupAbandonedRooms
);

// ---------------------------------------------------------------------------
// Weekly global league
// ---------------------------------------------------------------------------

/**
 * Calendar-month period id, matching LeaguePeriod.periodIdFor in the Android
 * app exactly: year*12 + (month - 1). The two MUST agree — a player's
 * profile is stamped with the app's id and this function filters on it.
 *
 * Evaluated in Istanbul, like every other schedule here. A player in another
 * timezone rolls over a few hours out of step with the table, which is a
 * cosmetic skew on a monthly number, and the only alternative — a per-player
 * month — cannot be aggregated at all.
 */
const LEAGUE_TIME_ZONE = "Europe/Istanbul";

interface IstanbulNow {
  year: number;
  month: number;   // 1-12
  day: number;
  hour: number;
  daysInMonth: number;
}

function istanbulNow(now: Date): IstanbulNow {
  const parts = new Intl.DateTimeFormat("en-CA", {
    timeZone: LEAGUE_TIME_ZONE,
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    hourCycle: "h23",
  }).formatToParts(now);
  const get = (type: string) => Number(parts.find((p) => p.type === type)?.value ?? 0);
  const year = get("year");
  const month = get("month");
  // Day 0 of the next month is the last day of this one.
  const daysInMonth = new Date(Date.UTC(year, month, 0)).getUTCDate();
  return { year, month, day: get("day"), hour: get("hour"), daysInMonth };
}

function periodIdFor(t: IstanbulNow): number {
  return t.year * 12 + (t.month - 1);
}

/**
 * The prize id for a period, derived rather than configured: the artwork is
 * stamped with its own month, so there is exactly one right answer and
 * nobody has to remember to set it. Matches AvatarFrame's
 * LEAGUE_CHAMPION_<year>_<month> constants and LeagueReward's FRAME: prefix.
 *
 * A month whose artwork this build of the app does not ship still gets an id
 * recorded; the app resolves it to nothing and shows no prize rather than
 * the wrong one, and a later update reveals it.
 */
function rewardIdFor(periodId: number): string {
  const year = Math.floor(periodId / 12);
  const month = (periodId % 12) + 1;
  return `FRAME:LEAGUE_CHAMPION_${year}_${String(month).padStart(2, "0")}`;
}

/** Entries in the published snapshot. A bot row carries no uid — see buildGlobalLeaderboard. */
interface LeagueRow {
  uid: string | null;
  nickname: string;
  periodXp: number;
  level: number;
  bot: boolean;
}

/**
 * Deterministic PRNG (mulberry32). Bots must produce the SAME identity and a
 * monotonically growing score on every rebuild — a bot whose name or score
 * jumped around between refreshes would be obvious within a day.
 */
function seededRandom(seed: number): () => number {
  let a = seed >>> 0;
  return () => {
    a = (a + 0x6d2b79f5) >>> 0;
    let t = Math.imul(a ^ (a >>> 15), 1 | a);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

// Real-looking usernames, not a prefix+suffix generator: a generator built
// from a small word bank (kalemusta23, boyaavcı45, ...) always shares one
// obvious theme, which is exactly how a player works out a name is a bot.
// Half title-cased and half not, same as actual handles sitting next to each
// other — kept in sync with GhostPersonas.NICKNAMES_TR in the Android app
// (BotGhostRun.kt), the same list for Hızlı Eşleş's synthesised opponents.
const BOT_NAME_POOL = [
  "Memetcan", "ahmet734", "Fthylmz", "uykuluadam",
  "Kraduman", "fistikezmesi", "Kadir007", "ceyda8821",
  "Cananabaci", "yussuf", "Deliomer", "ruzgargibi",
  "Brkydmr", "burakreis", "Gozluklucocuk", "mustfcn",
  "Sagocu99", "yalnizkurt", "Ahmmet", "asabiadam",
  "Simitcay", "kaptanali", "Karabela", "gecebekcisi",
  "Demirhan", "hknkrks", "Yorgunsavasci", "zynpcetn",
  "Karakoc", "aysenur11", "Alican1903", "siyahinci",
  "Ssknr", "mertcn", "Iremsu", "bsgul",
  "Aleyna34", "gorkem543", "Cnsyksl", "bthnky",
  "Yusufinho", "polatalmdr", "Minikkus", "gamsizbaykus",
  "Mimarmerve", "muhendisbey", "Soforkemal", "issizgucsuz",
  "Mezunadam", "caykolik", "Kemalkaya", "gizemlikiz",
  "Kafkef88", "poyrazkarayel", "Ucanbalik", "isimsizkahraman",
  "Kacakyolcu", "delidolu", "Yalnizim", "firtinakemal",
  "Gocebe", "krmzblt", "Karadenizli", "vethasan",
  "Volkan00", "keloglan", "Gulyabani", "tosuncuk",
  "Karaeylem", "ogretmenim", "Hemsiremelisa", "avukatbey",
  "Ogrenciyiz", "tekbasina", "Krdsler", "sariyildiz",
  "Merve742", "farukeczanesi", "Cemal33", "komsukizi",
  "Bakkalamca", "uykucu", "Sessizkalan", "gokhantepe",
  "Ahemt98", "yanlizadam", "Herkezgitsin", "orjinall",
  "Suprizci", "yalnizdegil", "Mnyk", "fth123",
  "Qweasd", "tofask", "Passatci", "hondacivic",
  "Cbf150", "broadwayci", "Doganslx", "izmir35",
  "Bursa1616", "kordonboyu", "Kemalpasali", "mudanyali",
  "Adana01", "cikkofteci", "Caykasigi", "sekersiz",
  "Bolacili", "sarmisakli", "Uykumvar", "nebilimben",
  "Bosver", "falanfilan", "Ivirzivir", "baksanabana",
  "Belkide", "veterinerbey", "Yirmi8", "hekimsami",
  "98tayfa", "mormadenci", "Ustaeller", "kafkef",
  "Pesimist", "cimbom1905", "Fenerli1907", "bjk1903",
  "Ronaldo7", "ts61", "Messi10", "spinci",
  "Lufersesi", "amatorbalikci", "Sahteyem", "yagmurlu",
  "Lodos", "ametist", "Hsncn", "brk98",
  "Glsh", "mstyfa", "Ahmet8520", "cufcuf",
  "Wqewqe", "bumbum", "Laylaylom", "laylon",
  "Soley", "hicbiri", "Sonsoz", "oburki",
  "Isimsiz", "siyahgiyen", "Heryeryesil", "kdr",
  "Gokhn", "voldemort", "Padisah", "vezir",
  "Kayiboyu", "ineksaban",
  "enesk", "Melihcan", "gokalp07", "Zeynepnaz",
  "tarikk", "Sevgiliyim", "bulentbey", "Aycan_m",
  "muratcan55", "Ferhatt", "duygu_k", "Salihk1",
  "emirhann", "Beratcan", "kubrag", "Ozanbey",
  "aslihan99", "Yigitcan", "ercank", "Tuncerbey",
  "birkank", "Kayahan_", "ediz34", "Melisnur",
  "hakanaltin", "Sumeyye_k", "canerk35", "Ilayda_r",
  "yakupp", "Selimcan", "meltemx", "huseyinkoc",
  "Gulcan55", "ridvank", "Ipeknur", "tolgaa",
  "Berkant", "ferideh", "Cansu_yz", "mucahitt",
  "Idilnaz", "atillaa", "semihk", "Zeliha_t",
  "onurcan", "Basakk", "irfanbey", "Necla_h",
  "turgutt", "Ozlemk", "kenanaydin", "Ebruc",
  "savasbey", "Yeliz_d", "erolk", "Sibelnur",
  "kadircan", "Nese_y", "mahiryilmaz", "Damla_ceyy",
];

/**
 * One name per bot for the whole period, with no two bots sharing one —
 * a Fisher-Yates shuffle of the whole pool, seeded purely by periodId so
 * every rebuild within the month lands on the same assignment. Picking
 * each bot's name independently (one seededRandom draw per bot) very
 * likely collided somewhere: 25 draws out of a 162-name pool is well past
 * the birthday-paradox threshold, and two bots sharing a name on the same
 * leaderboard is a bigger tell than any individual name ever was.
 */
function botNicknamesForPeriod(periodId: number, count: number): string[] {
  const pool = [...BOT_NAME_POOL];
  const random = seededRandom(periodId);
  for (let i = pool.length - 1; i > 0; i--) {
    const j = Math.floor(random() * (i + 1));
    [pool[i], pool[j]] = [pool[j], pool[i]];
  }
  return pool.slice(0, count);
}

/**
 * How many filler rows the table carries. Equal to [PUBLISHED_TABLE_SIZE] on
 * purpose: with one fewer, the single spare slot was always taken by the
 * top REAL player however little they had scored, so a player nowhere near
 * the top 25 saw themselves parked on row 25.
 */
const BOT_COUNT = 25;
// How many rows the real-player query fetches, before bots are mixed in and
// the combined list is cut down to PUBLISHED_TABLE_SIZE below. Generous on
// purpose: a real player ranked, say, 40th by raw XP still needs to be IN
// this fetch for the final sort-then-slice to have a chance of seating them
// ahead of a bot.
const MAX_ENTRIES = 100;
// The actual ceiling on what the app ever shows. Real players and bots are
// sorted together by periodXp and only the top PUBLISHED_TABLE_SIZE survive
// the cut — so on a quiet month this is BOT_COUNT bots plus however many
// real players outscored the weakest bot, never more than this many rows
// total, whatever the real player count turns out to be.
const PUBLISHED_TABLE_SIZE = 25;

/**
 * Random XP a bot gains each time growth is applied — see
 * [runBuildGlobalLeaderboard]. A sixth of the original 200-1000 (per the
 * schedule moving from every 6 hours to every 1), so the DAILY total a bot
 * earns stays the same — only how finely it's spread across the day changed.
 */
const BOT_GROWTH_MIN = 35;
const BOT_GROWTH_MAX = 165;

/**
 * The level a bot's card shows, derived from its own periodXp instead of a
 * random draw independent of it (see the fix note on the call site below).
 *
 * Mirrors PlayerLevel.levelForXp/totalXpForLevel on the Android side
 * (totalXpForLevel(level) = 25*(level-1)^2 + 75*(level-1)) — closed-form
 * inverse of that quadratic, capped the same way. Keep the two in sync if
 * the curve ever changes; nothing here enforces that automatically (same
 * caveat as every other "keep in sync" constant in this file).
 */
const BOT_MAX_LEVEL = 100;
function levelForBotXp(xp: number): number {
  if (xp <= 0) return 1;
  const n = Math.floor((-75 + Math.sqrt(75 * 75 + 100 * xp)) / 50);
  return Math.min(BOT_MAX_LEVEL, Math.max(1, n + 1));
}

/**
 * Minimum real time between two growth applications to the same bot. The
 * schedule this runs from fires every hour; 50 minutes gives headroom for a
 * manual or slightly-early re-run not to double a bot's growth, while never
 * missing a real hourly tick.
 */
const BOT_GROWTH_INTERVAL_MS = 50 * 60 * 1000;

interface BotState {
  nickname: string;
  periodXp: number;
  level: number;
}

/**
 * Publishes the whole global table as ONE document, every hour.
 *
 * The alternative — every client querying users/ directly — costs one read
 * per listed player per viewer. At a hundred listed players and four opens a
 * day that is forty thousand reads a day for a hundred players, which is
 * most of the free daily quota spent on a single screen. This way a viewer
 * pays ONE read, and the hundred reads happen here, 24 times a day, no
 * matter how many people look.
 *
 * **Bots exist only in this document.** Nothing is ever written to users/ for
 * them: a fake profile there would surface in friend search, in duels and in
 * every other place that reads a real account, and would be discovered the
 * first time somebody tried to add one. They carry no uid for the same
 * reason — the app refuses to open a profile for a row without one.
 *
 * **Bots accumulate, they are not recomputed from scratch.** Each bot's
 * identity (nickname, level) is deterministic from `periodId` and its own
 * index, exactly as before — the same bot never renames itself mid-month.
 * Its score, though, is carried forward from the previous snapshot and grows
 * by a random [BOT_GROWTH_MIN, BOT_GROWTH_MAX] every time this runs
 * (throttled by [BOT_GROWTH_INTERVAL_MS] so a manual or early re-run does not
 * double-grant growth). This is deliberately NOT capped below the real
 * podium the way an earlier version was: bots are meant to be real
 * competition — a player who stops playing gets overtaken, and the table
 * keeps moving even with no human activity at all. The actual prize is
 * unaffected either way: [runFinalizeLeaguePeriod] picks winners straight
 * from `users/`, never from this table, so a bot sitting in the visible top
 * three still cannot win anything.
 */
// Not currently deployed as a Cloud Function — see functions/DEPLOY.md and
// the note on runCleanupAbandonedRooms above. Runs from
// .github/workflows/league-scheduler.yml instead, on the same schedule.
export const buildGlobalLeaderboard = onSchedule(
  { schedule: "every 1 hours", timeZone: LEAGUE_TIME_ZONE },
  runBuildGlobalLeaderboard
);

export async function runBuildGlobalLeaderboard(): Promise<void> {
  const db = admin.firestore();
  const t = istanbulNow(new Date());
  const periodId = periodIdFor(t);

    const realSnapshot = await db
      .collection("users")
      .where("periodId", "==", periodId)
      .orderBy("periodXp", "desc")
      .limit(MAX_ENTRIES)
      .get();

    const real: LeagueRow[] = realSnapshot.docs.map((doc) => ({
      uid: doc.id,
      nickname: (doc.get("nickname") as string | undefined)?.trim() || "?",
      periodXp: (doc.get("periodXp") as number | undefined) ?? 0,
      level: (doc.get("level") as number | undefined) ?? 1,
      bot: false,
    }));

    // Carried INTO the snapshot rather than read separately by every client:
    // the reward of the week and last week's winners then cost nothing to
    // look at, because the table was going to be read anyway.
    const config = await db.doc("leaderboards/config").get();
    const previous = await db.doc("leaderboards/global").get();

    const previousPeriodId = previous.get("periodId") as number | undefined;
    const previousBots = (previous.get("bots") as BotState[] | undefined) ?? [];
    const previousBotsGrewAt = previous.get("botsGrewAt") as number | undefined;

    const samePeriod = previousPeriodId === periodId;
    const now = Date.now();
    // A new month starts every bot back at zero, same as a real player's own
    // periodXp — growth is then due immediately so the table is not all
    // zeroes right after the rollover.
    const growthDue =
      !samePeriod || previousBotsGrewAt === undefined || now - previousBotsGrewAt >= BOT_GROWTH_INTERVAL_MS;

    const bots: LeagueRow[] = [];
    const botStates: BotState[] = [];
    const nicknames = botNicknamesForPeriod(periodId, BOT_COUNT);
    for (let i = 0; i < BOT_COUNT; i++) {
      const nickname = nicknames[i];

      let periodXp = samePeriod ? previousBots[i]?.periodXp ?? 0 : 0;
      if (growthDue) {
        // Seeded by the growth tick rather than pure Math.random(): two
        // calls landing in the same throttle window (retries, a manual
        // re-run right after the scheduled one) compute the same increment
        // instead of each adding their own.
        const tick = Math.floor(now / BOT_GROWTH_INTERVAL_MS);
        const growth = seededRandom(tick * 104_729 + periodId * 97 + i);
        periodXp += BOT_GROWTH_MIN + Math.floor(growth() * (BOT_GROWTH_MAX - BOT_GROWTH_MIN + 1));
      }
      // Was an independent random draw (2-62, fixed for the whole period
      // regardless of periodXp) — a bot with a low roll could sit on 8000+
      // XP by month's end while still showing as, say, level 9, which is
      // exactly the "how did they get 8280 XP at level 9" implausibility
      // players notice. Derived from periodXp instead, so the level shown
      // always matches the XP shown next to it.
      const level = levelForBotXp(periodXp);

      bots.push({ uid: null, nickname, periodXp, level, bot: true });
      botStates.push({ nickname, periodXp, level });
    }

    const entries = [...real, ...bots]
      .sort((a, b) => b.periodXp - a.periodXp || a.nickname.localeCompare(b.nickname))
      .slice(0, PUBLISHED_TABLE_SIZE);

    // On the first build of a new month, keep the standings the table showed
    // just before the rollover. A player's own profile is re-stamped with the
    // new month the moment they next open the app, which takes them out of
    // finalizeLeaguePeriod's users/ query — and the most active players (the
    // ones who win) are exactly the ones who open it right after midnight.
    // These rows are the safety net that keeps their final score countable.
    const endedPeriod =
      !samePeriod && previousPeriodId !== undefined
        ? {
            periodId: previousPeriodId,
            entries: ((previous.get("entries") as LeagueRow[] | undefined) ?? []).filter((e) => !e.bot && e.uid),
          }
        : undefined;

    // merge: true — this used to overwrite the whole document, and copied
    // lastPeriod across from a read taken earlier in the run. A finalize that
    // committed in between was then silently undone, taking the winners (and
    // with them the prize) back off the table.
    await db.doc("leaderboards/global").set(
      {
        periodId,
        generatedAt: now,
        daysRemaining: Math.max(t.daysInMonth - t.day, 0),
        // The month's own prize, unless the review panel has overridden it.
        rewardId: (config.get("rewardId") as string | undefined) ?? rewardIdFor(periodId),
        entries,
        bots: botStates,
        botsGrewAt: growthDue ? now : previousBotsGrewAt ?? now,
        ...(endedPeriod ? { endedPeriod } : {}),
      },
      { merge: true }
    );

  logger.info(
    `League: ${real.length} real + ${bots.length} bot row(s) for period ${periodId}, growth ${growthDue ? "applied" : "skipped"}`
  );
}

/**
 * Closes the month that just ended and records its top three.
 *
 * Runs a few minutes after midnight on the first, while every profile still
 * carries LAST month's id and final score — a player's own device only resets
 * its weekly total the next time it is opened, which is exactly what makes
 * the final standings still readable here.
 *
 * **Bots cannot win.** Only real accounts are considered, so the prize always
 * reaches a person even in a week where filler rows sat high in the table.
 *
 * Winners are recorded twice on purpose: in the public snapshot, where the
 * app finds them for free on a screen it was already reading, and durably
 * under each winner's own profile, so somebody who does not open the app for
 * a fortnight still collects what they won.
 *
 * Scheduled daily rather than for one exact moment on the 1st — the cron
 * this replaced fired weekly, a leftover from before the league moved to
 * calendar months, which meant it was closing "last month" fresh every
 * Monday instead of once at the actual boundary. A day is not a moment
 * either, and Firebase Scheduler's `timeZone` option (or, for the GitHub
 * Actions cron this currently runs from instead, no timezone support at
 * all) both make "the 1st in Istanbul" awkward to target exactly in UTC
 * cron fields. Running once a day and relying on the idempotency check
 * below sidesteps that entirely: [finishedPeriodId] is constant for every
 * day of a given month, so the write only actually happens once, on
 * whichever day this next runs on or after the real boundary.
 */
// Not currently deployed as a Cloud Function — see the note on
// runCleanupAbandonedRooms above. Runs from
// .github/workflows/league-scheduler.yml instead.
export const finalizeLeaguePeriod = onSchedule(
  { schedule: "every day 00:10", timeZone: LEAGUE_TIME_ZONE },
  runFinalizeLeaguePeriod
);

export async function runFinalizeLeaguePeriod(): Promise<void> {
  const db = admin.firestore();
  const finishedPeriodId = periodIdFor(istanbulNow(new Date())) - 1;

  const existing = await db.doc("leaderboards/global").get();
  if ((existing.get("lastPeriod") as { periodId?: number } | undefined)?.periodId === finishedPeriodId) {
    logger.info(`League: period ${finishedPeriodId} already finalized, skipping`);
    return;
  }

  const config = await db.doc("leaderboards/config").get();
    const rewardId = (config.get("rewardId") as string | undefined)
      ?? rewardIdFor(finishedPeriodId);

    const snapshot = await db
      .collection("users")
      .where("periodId", "==", finishedPeriodId)
      .orderBy("periodXp", "desc")
      .limit(3)
      .get();

    // Two sources, best score per player. The users/ query misses anyone who
    // opened the app after the rollover but before this ran (their profile
    // already carries the new month); the snapshot the table published just
    // before the rollover still has them. See runBuildGlobalLeaderboard.
    const candidates = new Map<string, { uid: string; nickname: string; periodXp: number }>();
    const consider = (uid: string, nickname: string, periodXp: number) => {
      const seen = candidates.get(uid);
      if (!seen || periodXp > seen.periodXp) candidates.set(uid, { uid, nickname, periodXp });
    };
    for (const doc of snapshot.docs) {
      consider(
        doc.id,
        (doc.get("nickname") as string | undefined)?.trim() || "?",
        (doc.get("periodXp") as number | undefined) ?? 0
      );
    }
    const ended = existing.get("endedPeriod") as { periodId?: number; entries?: LeagueRow[] } | undefined;
    if (ended?.periodId === finishedPeriodId) {
      for (const row of ended.entries ?? []) {
        if (row.uid && !row.bot) consider(row.uid, row.nickname?.trim() || "?", row.periodXp ?? 0);
      }
    }

    const winners = [...candidates.values()]
      .filter((c) => c.periodXp > 0)
      .sort((a, b) => b.periodXp - a.periodXp || a.nickname.localeCompare(b.nickname))
      .slice(0, 3)
      .map((c, index) => ({ ...c, rank: index + 1 }));

    const batch = db.batch();
    for (const winner of winners) {
      batch.set(
        db.doc(`users/${winner.uid}/private/leagueAwards`),
        { [String(finishedPeriodId)]: { rank: winner.rank, rewardId, periodXp: winner.periodXp } },
        { merge: true }
      );
    }
    batch.set(
      db.doc("leaderboards/global"),
      { lastPeriod: { periodId: finishedPeriodId, rewardId, winners } },
      { merge: true }
    );
    await batch.commit();

  logger.info(`League: period ${finishedPeriodId} closed with ${winners.length} winner(s), prize ${rewardId}`);
}

// ---------------------------------------------------------------------------
// Referral rewards
// ---------------------------------------------------------------------------

/**
 * XP granted to whoever sent a friend-invite link once the person who opened
 * it reaches this level. Keep in sync with FriendsScreen's
 * friends_invite_reward_hint / InviteShareUtil's share_friend_reward_hint on
 * the Android side — there is no shared source of truth between the two,
 * this project has no build step that could enforce one.
 */
const REFERRAL_REWARD_XP = 500;
const REFERRAL_REWARD_MIN_LEVEL = 5;
// Both closed the same way an audit flagged this pipeline as farmable:
// `level` is a client-published field (see users/{uid} in firestore.rules)
// with no proof the invitee ever actually played to earn it — a script
// could create a throwaway anon account, stamp invitedByUid, then
// immediately publish level: 5 and collect 500 XP with zero real gameplay,
// repeated without limit for one inviter.
//
// MIN_ACCOUNT_AGE_MS makes that require real wall-clock time per fake
// account instead of being free — a genuine new player reaching level 5
// takes far longer than this in practice, so it costs nobody honest
// anything, while turning "run a script" into "wait a day per fake account".
const REFERRAL_REWARD_MIN_ACCOUNT_AGE_MS = 24 * 60 * 60 * 1000;
// A cap on how many rewards one inviter can ever collect — generous enough
// that no real player's genuine friend circle hits it, but it bounds the
// worst case of the exploit above to a fixed, small amount of stolen XP
// instead of an unbounded farm.
const REFERRAL_REWARD_MAX_PER_INVITER = 30;

/**
 * Pays out the referral reward once an invitee reaches level 5.
 *
 * XP is client-authoritative (see SettingsRepository.kt / LeagueScorePublisher
 * on the Android side): a level or periodXp written straight onto the
 * inviter's own users/{uid} document here would just be overwritten the next
 * time their own device republishes its real total. So this never touches the
 * inviter's XP directly — it drops one entry into their private/pendingRewards
 * document instead (admin credentials bypass firestore.rules' otherwise
 * owner-only write there), which their own app reads and applies to its local
 * XP the next time it starts (see ReferralRewardClaimer.kt).
 *
 * invitedByUid / referralRewardGranted are written once, by the INVITEE's own
 * device, the moment it opens a friend-invite link (see
 * FriendRepositoryImpl.recordReferralIfEligible) — referralRewardGranted is
 * stamped false in that same write (not left absent) so the `==` filter below
 * can find it at all.
 */
// Not currently deployed as a Cloud Function — see the note on
// runCleanupAbandonedRooms above. Runs from
// .github/workflows/league-scheduler.yml instead.
export const grantReferralRewards = onSchedule(
  { schedule: "every day 03:00", timeZone: LEAGUE_TIME_ZONE },
  runGrantReferralRewards
);

export async function runGrantReferralRewards(): Promise<void> {
  const db = admin.firestore();

  const snapshot = await db
    .collection("users")
    .where("referralRewardGranted", "==", false)
    .where("level", ">=", REFERRAL_REWARD_MIN_LEVEL)
    .get();

  // Counts how many times each inviter has already been paid, so the
  // per-inviter cap below can be enforced across this whole run without a
  // Firestore read per candidate — cheap since a farming attempt is the one
  // case this map grows large, and that is exactly what it exists to stop.
  const grantedByInviter = new Map<string, number>();

  let granted = 0;
  let skippedTooNew = 0;
  let skippedCapped = 0;
  for (const doc of snapshot.docs) {
    const inviterUid = doc.get("invitedByUid") as string | undefined;
    if (!inviterUid) continue;

    const alreadyGranted = grantedByInviter.get(inviterUid) ?? 0;
    if (alreadyGranted >= REFERRAL_REWARD_MAX_PER_INVITER) {
      skippedCapped++;
      continue;
    }

    // The invitee's own AUTH account creation time — not any Firestore
    // field, which the client controls — so this can't be spoofed by
    // publishing a field early.
    const authUser = await admin.auth().getUser(doc.id).catch(() => null);
    const createdAtMs = authUser ? Date.parse(authUser.metadata.creationTime) : 0;
    if (!createdAtMs || Date.now() - createdAtMs < REFERRAL_REWARD_MIN_ACCOUNT_AGE_MS) {
      skippedTooNew++;
      continue;
    }

    const batch = db.batch();
    batch.update(doc.ref, { referralRewardGranted: true });
    batch.set(
      db.doc(`users/${inviterUid}/private/pendingRewards`),
      {
        pending: admin.firestore.FieldValue.arrayUnion({
          amount: REFERRAL_REWARD_XP,
          reason: "referral",
          sourceUid: doc.id,
          createdAt: Date.now(),
        }),
      },
      { merge: true }
    );
    await batch.commit();
    grantedByInviter.set(inviterUid, alreadyGranted + 1);
    granted++;
  }

  logger.info(
    `Referral rewards: granted ${granted} of ${snapshot.size} eligible invitee(s) ` +
      `(${skippedTooNew} too-new account(s), ${skippedCapped} over the per-inviter cap)`
  );
}
