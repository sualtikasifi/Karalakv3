package com.sualtikasifi.cizimhafiza.domain.repository

import com.sualtikasifi.cizimhafiza.domain.model.Coupon
import com.sualtikasifi.cizimhafiza.domain.model.RedeemResult

interface CouponRepository {
    /**
     * Uses a code. The server-side transaction checks that it exists, has not expired, has uses left and has not
     * been used by this account yet, and only then counts the use.
     */
    suspend fun redeem(rawCode: String): RedeemResult

    /** The code as it stands (with how often it has been used), or null when there is none: to not replace one by mistake. */
    suspend fun find(code: String): Result<Coupon?>

    /**
     * Developer panel only (the rules allow nobody else): creates a code, in one transaction with its audit-log line.
     * Never replaces one: a name already taken fails with [com.sualtikasifi.cizimhafiza.domain.model.CouponExistsException],
     * so a typed code can no longer wipe the use count and the reward of a code already given out.
     */
    suspend fun create(coupon: Coupon): Result<Unit>

    /** Developer panel only: the newest codes with how often each has been used. */
    suspend fun recent(limit: Int): Result<List<Coupon>>
}
