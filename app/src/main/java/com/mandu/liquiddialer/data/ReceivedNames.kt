package com.mandu.liquiddialer.data

import android.content.Context
import android.telecom.Call
import android.telecom.TelecomManager

/** Private, unverified names supplied to this app by Android Telecom. Never exported in settings backup. */
object ReceivedNames {
    private fun prefs(ctx: Context) = ctx.getSharedPreferences("received_caller_names", Context.MODE_PRIVATE)
    private fun key(number: String) = ContactControls.k(number)
    fun get(ctx: Context, number: String): String? = key(number).takeIf { it.isNotEmpty() }?.let { prefs(ctx).getString(it, null) }
    fun clear(ctx: Context, number: String) { val k=key(number); if(k.isNotEmpty()) prefs(ctx).edit().remove(k).apply() }
    fun dismiss(ctx: Context, number: String) {
        val k=key(number); if(k.isEmpty()) return
        val old=get(ctx,number)
        prefs(ctx).edit().apply { remove(k); if(old!=null) putString("dismissed:$k",old) }.apply()
    }
    internal fun learn(ctx: Context, number: String, name: String?, numberPresentation: Int, namePresentation: Int) {
        if(numberPresentation != TelecomManager.PRESENTATION_ALLOWED || namePresentation != TelecomManager.PRESENTATION_ALLOWED) return
        val k=key(number); val clean=name?.trim()?.takeIf { it.isNotBlank() && it.length<=160 && it.none { ch -> ch.isISOControl() } } ?: return
        if(prefs(ctx).getString("dismissed:$k",null)==clean) return
        if(k.isEmpty() || clean.filter { it.isDigit() } == number.filter { it.isDigit() }) return
        if(prefs(ctx).getString(k,null)!=clean) prefs(ctx).edit().putString(k,clean).apply()
    }
    fun observe(ctx: Context, call: Call) {
        val d=call.details ?: return
        val n=d.handle?.schemeSpecificPart ?: return
        if(Repo.lookupName(ctx,n)!=null) { clear(ctx,n); return }
        if(android.os.Build.VERSION.SDK_INT >= 29 && d.callDirection == Call.Details.DIRECTION_OUTGOING) return
        learn(ctx,n,d.callerDisplayName,d.handlePresentation,d.callerDisplayNamePresentation)
    }
    /** Contact is authoritative. Saving it removes the learned record so it cannot reappear after deletion. */
    fun display(ctx: Context, number: String): String? {
        if(get(ctx,number)==null) return null
        val saved=Repo.lookupName(ctx,number)
        if(saved!=null) { clear(ctx,number); return saved.first.takeIf { it.isNotBlank() } }
        return get(ctx,number)
    }
}
