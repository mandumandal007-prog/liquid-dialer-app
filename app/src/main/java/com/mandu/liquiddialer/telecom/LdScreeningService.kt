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
        val block = incoming && ((s.blockPrivate && n.isBlank()) || (s.blockUnknown && n.isNotBlank() && hasContacts && Repo.lookupName(this, n) == null) || (n.isNotBlank() && SeriesBlock.matches(n, s.blockedPrefixes)))
        val cal = java.util.Calendar.getInstance()
        val nowMin = cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
        // Contacts permission ke bina favourite check nahi ho sakta: bypass ON hai to silence mat karo (favourite miss na ho)
        val favUnchecked = s.quietAllowFav && n.isNotBlank() && !hasContacts
        val fav = n.isNotBlank() && hasContacts && Repo.contactStarred(this, n)
        val silence = !block && !favUnchecked && QuietHours.shouldSilence(s.quietHours, android.os.Build.VERSION.SDK_INT, incoming, nowMin, s.quietStart, s.quietEnd, s.quietAllowFav, fav)
        val r = CallResponse.Builder()
        if (block) r.setDisallowCall(true).setRejectCall(true).setSkipNotification(false).setSkipCallLog(false)
        if (silence) r.setSilenceCall(true)
        respondToCall(details, r.build())
    }
}
