package com.sualtikasifi.cizimhafiza.util

/**
 * The amounts the economy pays, with their designed defaults. EconomyConfigRepository copies the `config/economy`
 * document over them while the app runs, so the pace of gold can be tuned from the Firebase console (or Karalak Panel)
 * without an update. GameConstants reads these, so every place that pays gold follows the server's numbers.
 */
object EconomyConfig {
    @Volatile var goldPerCorrectWord: Int = 10
    @Volatile var onlineWinGold: Int = 100
    @Volatile var achievementGold: Int = 100
    @Volatile var levelGoldPerNewStar: Int = 50
    @Volatile var dailyGoldBase: Int = 300
    @Volatile var dailyGoldPerStreakDay: Int = 30
    @Volatile var chainBonusGold: Int = 10
    @Volatile var adGoldAmount: Int = 500
    @Volatile var adGoldCooldownHours: Int = 3
    @Volatile var adChestCooldownHours: Int = 4
    @Volatile var leagueMonthlyGold: Int = 10_000

    /** Every field, its document key and the range a server value must fall in to be taken (otherwise the default stays). */
    internal val fields: List<Field> = listOf(
        Field("goldPerCorrectWord", 1..1_000, 10) { goldPerCorrectWord = it },
        Field("onlineWinGold", 0..10_000, 100) { onlineWinGold = it },
        Field("achievementGold", 0..10_000, 100) { achievementGold = it },
        Field("levelGoldPerNewStar", 0..10_000, 50) { levelGoldPerNewStar = it },
        Field("dailyGoldBase", 0..100_000, 300) { dailyGoldBase = it },
        Field("dailyGoldPerStreakDay", 0..10_000, 30) { dailyGoldPerStreakDay = it },
        Field("chainBonusGold", 0..1_000, 10) { chainBonusGold = it },
        Field("adGoldAmount", 0..100_000, 500) { adGoldAmount = it },
        Field("adGoldCooldownHours", 1..48, 3) { adGoldCooldownHours = it },
        Field("adChestCooldownHours", 1..48, 4) { adChestCooldownHours = it },
        Field("leagueMonthlyGold", 0..1_000_000, 10_000) { leagueMonthlyGold = it }
    )

    internal class Field(val key: String, val range: IntRange, val default: Int, val set: (Int) -> Unit)

    /** Takes the values of a `config/economy` document; a missing, wrong-typed or out-of-range field keeps its default. */
    fun apply(data: Map<String, Any?>?) {
        fields.forEach { field ->
            val value = (data?.get(field.key) as? Number)?.toInt()?.takeIf { it in field.range } ?: field.default
            field.set(value)
        }
    }
}
