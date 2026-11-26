package com.sualtikasifi.cizimhafiza.domain.model

/**
 * Where a piece of XP or gold came from. The device keeps a running total per source for the league month (see [EarnLog]) and
 * uploads it with the cloud save, so the developer panel can tell a player who climbs through play from one whose numbers arrive
 * from somewhere they should not. [key] is what is stored; [label] is what the panel shows.
 */
enum class EarnSource(val key: String, val label: String) {
    GAME("game", "Oyun (kelime ve bölüm)"),
    DAILY("daily", "Günün meydan okuması"),
    ONLINE("online", "Çevrimiçi maç"),
    AD("ad", "Reklam (çarpan, ücretsiz altın)"),
    CHEST("chest", "Kasa"),
    ACHIEVEMENT("achievement", "Başarım ödülü"),
    COUPON("coupon", "Kupon"),
    LOGIN("login", "Günlük giriş"),
    LEAGUE("league", "Lig ödülü"),
    REFERRAL("referral", "Davet"),
    FRIEND_STREAK("friendstreak", "Arkadaş serisi"),
    GUESS("guess", "Bil Bakalım"),
    OTHER("other", "Diğer");

    /**
     * Whether XP from this source counts towards the monthly league. The league is a month of PLAYING: windfalls — an
     * achievement's reward (up to thousands of XP at once), a coupon, a login day, a referral, a friend-streak prize, a chest, last
     * month's league prize — raise the player's level but not their place in the table.
     */
    val countsForLeague: Boolean
        get() = this != ACHIEVEMENT && this != COUPON && this != LOGIN && this != REFERRAL && this != FRIEND_STREAK && this != CHEST && this != LEAGUE

    companion object {
        fun labelOf(key: String): String = entries.firstOrNull { it.key == key }?.label ?: key
    }
}
