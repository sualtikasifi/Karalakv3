package com.sualtikasifi.cizimhafiza.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.sualtikasifi.cizimhafiza.domain.model.ChestTier
import com.sualtikasifi.cizimhafiza.domain.model.Coupon
import com.sualtikasifi.cizimhafiza.domain.model.CouponCodes
import com.sualtikasifi.cizimhafiza.domain.model.RedeemResult
import com.sualtikasifi.cizimhafiza.domain.repository.CouponRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Firestore layout: coupons/{CODE} — { gold, xp, chest, maxUses, usedCount, expiresAt, createdAt } — and
 * coupons/{CODE}/redemptions/{uid} — { redeemedAt }, one per account, which is what stops a code being used twice
 * by the same person. See firestore.rules: the use counter can only go up by one, and only together with that
 * redemption document, so a code cannot be used more often than its [Coupon.maxUses] allows.
 */
class CouponRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : CouponRepository {

    private val coupons get() = firestore.collection("coupons")

    private suspend fun requireUid(): String {
        auth.currentUser?.uid?.let { return it }
        return auth.signInAnonymously().await().user?.uid ?: throw IllegalStateException("auth-failed")
    }

    override suspend fun redeem(rawCode: String, chestSlotFree: Boolean): RedeemResult {
        val code = CouponCodes.normalize(rawCode) ?: return RedeemResult.Invalid
        return try {
            val uid = requireUid()
            val couponRef = coupons.document(code)
            val redemptionRef = couponRef.collection("redemptions").document(uid)
            firestore.runTransaction<RedeemResult> { tx ->
                val snapshot = tx.get(couponRef)
                if (!snapshot.exists()) return@runTransaction RedeemResult.NotFound
                val coupon = snapshot.toCoupon()
                val now = System.currentTimeMillis()
                if (coupon.expiresAtMillis > 0 && now >= coupon.expiresAtMillis) return@runTransaction RedeemResult.Expired
                if (coupon.usedCount >= coupon.maxUses) return@runTransaction RedeemResult.Exhausted
                if (tx.get(redemptionRef).exists()) return@runTransaction RedeemResult.AlreadyUsed
                if (coupon.chest != null && !chestSlotFree) return@runTransaction RedeemResult.ChestSlotsFull
                tx.set(redemptionRef, mapOf("redeemedAt" to now))
                tx.update(couponRef, "usedCount", (coupon.usedCount + 1).toLong())
                RedeemResult.Success(coupon.copy(usedCount = coupon.usedCount + 1))
            }.await()
        } catch (e: Exception) {
            RedeemResult.NetworkError
        }
    }

    override suspend fun create(coupon: Coupon): Result<Unit> = runCatching {
        requireUid()
        coupons.document(coupon.code).set(
            mapOf(
                "gold" to coupon.gold.toLong(),
                "xp" to coupon.xp.toLong(),
                "chest" to (coupon.chest?.name ?: ""),
                "maxUses" to coupon.maxUses.toLong(),
                "usedCount" to 0L,
                "expiresAt" to coupon.expiresAtMillis,
                "createdAt" to System.currentTimeMillis()
            )
        ).await()
    }

    override suspend fun recent(limit: Int): Result<List<Coupon>> = runCatching {
        coupons.orderBy("createdAt", Query.Direction.DESCENDING).limit(limit.toLong()).get().await()
            .documents.map { it.toCoupon() }
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toCoupon() = Coupon(
        code = id,
        gold = (getLong("gold") ?: 0L).toInt().coerceIn(0, CouponCodes.MAX_GOLD),
        xp = (getLong("xp") ?: 0L).toInt().coerceIn(0, CouponCodes.MAX_XP),
        chest = getString("chest")?.let { name -> ChestTier.entries.find { it.name == name } },
        maxUses = (getLong("maxUses") ?: 1L).toInt(),
        usedCount = (getLong("usedCount") ?: 0L).toInt(),
        expiresAtMillis = getLong("expiresAt") ?: 0L
    )
}
