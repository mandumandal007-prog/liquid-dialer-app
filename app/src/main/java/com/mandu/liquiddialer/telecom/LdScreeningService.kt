package com.mandu.liquiddialer.telecom

import android.telecom.Call
import android.telecom.CallScreeningService
import com.mandu.liquiddialer.LdApp
import com.mandu.liquiddialer.data.Repo

/** Used when the app also holds the call-screening role: rejects unknown / private callers if enabled. */
class LdScreeningService : CallScreeningService() {
    override fun onScreenCall(details: Call.Details) {
        val s = LdApp.settings.value
        val n = details.handle?.schemeSpecificPart ?: ""
        val incoming = android.os.Build.VERSION.SDK_INT < 29 || details.callDirection == Call.Details.DIRECTION_INCOMING
        val hasContacts = checkSelfPermission(android.Manifest.permission.READ_CONTACTS) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val privateHit = incoming && s.blockPrivate && n.isBlank()
        val unknownHit = incoming && s.blockUnknown && n.isNotBlank() && hasContacts && Repo.lookupName(this, n) == null
        val seriesHit = incoming && n.isNotBlank() && SeriesBlock.matches(n, s.blockedPrefixes)
        val block = privateHit || unknownHit || seriesHit
        val cal = java.util.Calendar.getInstance()
        val nowMin = cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
        // Contacts permission ke bina favourite check nahi ho sakta: bypass ON hai to silence mat karo (favourite miss na ho)
        val favUnchecked = s.quietAllowFav && n.isNotBlank() && !hasContacts
        val fav = n.isNotBlank() && hasContacts && Repo.contactStarred(this, n)
        // Per-number quiet bypass (user ne Number sheet me khud on kiya ho) - app ka apna prefs, contacts permission nahi chahiye
        val contactBypass = n.isNotBlank() && com.mandu.liquiddialer.data.ContactControls.quietAllow(this, n)
        val silence = !block && !favUnchecked && !contactBypass && QuietHours.shouldSilence(s.quietHours, android.os.Build.VERSION.SDK_INT, incoming, nowMin, s.quietStart, s.quietEnd, s.quietAllowFav, fav)
        val r = CallResponse.Builder()
        if (block) {
            r.setDisallowCall(true).setRejectCall(true).setSkipNotification(false).setSkipCallLog(false)
            com.mandu.liquiddialer.data.BlockLog.append(this, n, when {
                privateHit -> com.mandu.liquiddialer.data.BlockLog.REASON_PRIVATE
                unknownHit -> com.mandu.liquiddialer.data.BlockLog.REASON_UNKNOWN
                else -> com.mandu.liquiddialer.data.BlockLog.REASON_SERIES
            })
        }
        if (silence) r.setSilenceCall(true)
        respondToCall(details, r.build())
    }
}
