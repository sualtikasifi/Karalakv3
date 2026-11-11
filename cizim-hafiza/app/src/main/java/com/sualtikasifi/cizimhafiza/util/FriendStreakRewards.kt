package com.sualtikasifi.cizimhafiza.util

import com.sualtikasifi.cizimhafiza.domain.model.AvatarFrame
import com.sualtikasifi.cizimhafiza.domain.model.JokerType
import com.sualtikasifi.cizimhafiza.domain.model.PenSkin

/** What a friend-streak milestone pays each of the two friends. */
sealed interface StreakReward {
    data class Gold(val amount: Int) : StreakReward

    /** One joker of every kind. */
    data object Jokers : StreakReward

    data class Frame(val frame: AvatarFrame) : StreakReward

    data class Pen(val pen: PenSkin) : StreakReward
}

/**
 * The prizes of the friend streak, one per milestone (see FriendStreaks.MILESTONES). Each friend takes theirs on their
 * own phone, once per streak run. The frame and the pen are only ever won this way (see AvatarFrame.isFriendReward).
 */
object FriendStreakRewards {
    fun rewardFor(milestone: Int): StreakReward = when (milestone) {
        3 -> StreakReward.Gold(50)
        7 -> StreakReward.Gold(100)
        14 -> StreakReward.Jokers
        30 -> StreakReward.Frame(AvatarFrame.FRIENDSHIP)
        50 -> StreakReward.Gold(500)
        100 -> StreakReward.Pen(PenSkin.FRIENDSHIP)
        else -> StreakReward.Gold(25)
    }

    fun apply(settings: SettingsRepository, reward: StreakReward) {
        when (reward) {
            is StreakReward.Gold -> settings.earnGold(reward.amount)
            StreakReward.Jokers -> JokerType.entries.forEach { settings.addJoker(it, 1) }
            is StreakReward.Frame -> settings.grantStoreItem("frame:${reward.frame.name}")
            is StreakReward.Pen -> settings.grantStoreItem("pen:${reward.pen.name}")
        }
    }
}
