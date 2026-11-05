package com.mandu.liquiddialer.data

import android.content.ContentProviderOperation
import android.content.Context
import android.provider.ContactsContract
import android.provider.ContactsContract.AggregationExceptions
import android.provider.ContactsContract.RawContacts

/** Duplicate contacts ko SAFE tareeke se merge karna: Android ka "link" (AggregationExceptions).
 *  Koi contact DELETE nahi hota, koi data copy/overwrite nahi hota - contacts bas ek ke roop me
 *  dikhne lagte hain, aur "Alag karo" se waise ke waise wapas alag ho jaate hain (reversible).
 *  Ye Google Contacts ka wahi "link/unlink" mechanism hai. */
object ContactMerge {

    /** Ek merge/unlink operation: (AggregationExceptions TYPE, rawContactId1, rawContactId2). */
    data class MergeOp(val type: Int, val raw1: Long, val raw2: Long)

    /** Batch apply ka raasta - tests me fake lagta hai, production me content resolver. */
    interface Exec { fun apply(ops: List<MergeOp>): Boolean }

    class ResolverExec(private val ctx: Context) : Exec {
        override fun apply(ops: List<MergeOp>): Boolean {
            val batch = ArrayList<ContentProviderOperation>()
            ops.forEach { o ->
                batch.add(
                    ContentProviderOperation.newUpdate(AggregationExceptions.CONTENT_URI)
                        .withValue(AggregationExceptions.TYPE, o.type)
                        .withValue(AggregationExceptions.RAW_CONTACT_ID1, o.raw1)
                        .withValue(AggregationExceptions.RAW_CONTACT_ID2, o.raw2)
                        .build()
                )
            }
            return try { ctx.contentResolver.applyBatch(ContactsContract.AUTHORITY, batch).isNotEmpty() }
            catch (e: Exception) { false }
        }
    }

    /** Tests isko set karte hain. */
    var execOverride: Exec? = null
    /** Tests me linked-state reader override. */
    var linkedReaderOverride: ((Context) -> List<LinkedGroup>)? = null
    /** Tests me raw-id resolver override. */
    var rawIdsOverride: ((Context, Long) -> List<Long>)? = null

    private fun exec(ctx: Context): Exec = execOverride ?: ResolverExec(ctx)

    data class LinkedGroup(val contactId: Long, val rawIds: List<Long>)

    /** Ek contact (aggregate id) ke saare raw contact ids. */
    fun rawIds(ctx: Context, contactId: Long): List<Long> {
        rawIdsOverride?.let { return it(ctx, contactId) }
        val out = mutableListOf<Long>()
        try {
            ctx.contentResolver.query(
                RawContacts.CONTENT_URI, arrayOf(RawContacts._ID),
                "${RawContacts.CONTACT_ID}=? AND ${RawContacts.DELETED}=0", arrayOf("$contactId"), null
            )?.use { c -> while (c.moveToNext()) out.add(c.getLong(0)) }
        } catch (_: Exception) { }
        return out.distinct()
    }

    /** Adjacent-pair ops: group ke har contact ko agle se jodo (chain sabko ek rakhti hai). */
    fun linkOps(rawIdLists: List<List<Long>>): List<MergeOp> =
        pairOps(rawIdLists, AggregationExceptions.TYPE_KEEP_TOGETHER)

    fun unlinkOps(rawIdLists: List<List<Long>>): List<MergeOp> =
        pairOps(rawIdLists, AggregationExceptions.TYPE_KEEP_SEPARATE)

    private fun pairOps(rawIdLists: List<List<Long>>, type: Int): List<MergeOp> {
        val raws = rawIdLists.flatten().distinct()
        return (0 until raws.size - 1).map { i -> MergeOp(type, raws[i], raws[i + 1]) }
    }

    /** Group (contact ids) ko merge (link) karo. false = kuch fail (permission ya provider). */
    fun link(ctx: Context, contactIds: List<Long>): Boolean {
        if (contactIds.size < 2) return false
        val raws = contactIds.map { rawIds(ctx, it) }
        if (raws.flatten().size < 2) return false
        val ops = linkOps(raws)
        if (ops.isEmpty()) return false
        return exec(ctx).apply(ops)
    }

    /** Linked group ko wapas alag karo (undo). */
    fun unlink(ctx: Context, rawIdList: List<Long>): Boolean {
        if (rawIdList.size < 2) return false
        val ops = pairOps(listOf(rawIdList), AggregationExceptions.TYPE_KEEP_SEPARATE)
        if (ops.isEmpty()) return false
        return exec(ctx).apply(ops)
    }

    /** Abhi jo contacts linked hain (KEEP_TOGETHER exceptions se). contactId -> uske raw ids. */
    fun linkedGroups(ctx: Context): List<LinkedGroup> {
        linkedReaderOverride?.let { return it(ctx) }
        val byContact = LinkedHashMap<Long, LinkedHashSet<Long>>()
        try {
            ctx.contentResolver.query(
                AggregationExceptions.CONTENT_URI,
                arrayOf("contact_id", AggregationExceptions.RAW_CONTACT_ID1, AggregationExceptions.RAW_CONTACT_ID2),
                "${AggregationExceptions.TYPE}=?", arrayOf("${AggregationExceptions.TYPE_KEEP_TOGETHER}"), null
            )?.use { c ->
                while (c.moveToNext()) {
                    val set = byContact.getOrPut(c.getLong(0)) { LinkedHashSet() }
                    set.add(c.getLong(1)); set.add(c.getLong(2))
                }
            }
        } catch (_: Exception) { }
        return byContact.map { (cid, raws) -> LinkedGroup(cid, raws.toList()) }.filter { it.rawIds.size >= 2 }
    }
}
