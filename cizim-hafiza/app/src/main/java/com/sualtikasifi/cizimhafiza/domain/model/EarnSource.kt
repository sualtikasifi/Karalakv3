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

    companion object {
        fun labelOf(key: String): String = entries.firstOrNull { it.key == key }?.label ?: key
    }
}
