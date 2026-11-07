package com.sualtikasifi.cizimhafiza.util

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Transaction
import com.google.firebase.firestore.WriteBatch
import kotlinx.coroutines.tasks.await

/**
 * The developer panel's audit log (`adminAudit`), written so it cannot be lost: the entry is added to the very batch (or
 * transaction) that makes the change, so either both land or neither does. A line written afterwards, in a call of its own,
 * could fail on its own and leave a change nobody recorded.
 *
 * The keys are exactly what firestore.rules allows (action, target, detail, by, at; `at` the server's time).
 */
object AdminAudit {
    const val COLLECTION = "adminAudit"

    /** The longest [AdminAuditText.detail] kept, the same cap the log has always had. */
    const val MAX_DETAIL = 200

    /** One log line as the document stores it; [by] is the signed-in reviewer's address. */
    fun entry(by: String?, action: String, target: String, detail: String): Map<String, Any> = mapOf(
        "action" to action,
        "target" to AdminAuditText.target(target),
        "detail" to AdminAuditText.detail(detail),
        "by" to by.orEmpty(),
        "at" to FieldValue.serverTimestamp()
    )

    /** Adds the line to [batch]: it commits or fails together with the change already in it. */
    fun add(firestore: FirebaseFirestore, batch: WriteBatch, by: String?, action: String, target: String, detail: String) {
        batch.set(firestore.collection(COLLECTION).document(), entry(by, action, target, detail))
    }

    /**
     * Makes one change and its audit line as a single batch: [writes] puts the change into the batch, the line is added, and
     * both are committed together. Throws when the batch is refused, so the caller sees a failure, never half of one.
     */
    suspend fun commit(
        firestore: FirebaseFirestore, by: String?, action: String, target: String, detail: String,
        writes: (WriteBatch) -> Unit
    ) {
        val batch = firestore.batch()
        writes(batch)
        add(firestore, batch, by, action, target, detail)
        batch.commit().await()
    }

    /** Adds the line to [tx], same reasoning as [add]. */
    fun add(firestore: FirebaseFirestore, tx: Transaction, by: String?, action: String, target: String, detail: String) {
        tx.set(firestore.collection(COLLECTION).document(), entry(by, action, target, detail))
    }
}

/** What goes into a log line's text fields: plain strings kept short, so one odd value can never make the write fail. */
object AdminAuditText {
    fun target(value: String): String = value.trim().take(120)
    fun detail(value: String): String = value.trim().take(AdminAudit.MAX_DETAIL)
}
