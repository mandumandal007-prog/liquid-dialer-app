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
        val block = incoming && ((s.blockPrivate && n.isBlank()) || (s.blockUnknown && n.isNotBlank() && checkSelfPermission(android.Manifest.permission.READ_CONTACTS) == android.content.pm.PackageManager.PERMISSION_GRANTED && Repo.lookupName(this, n) == null))
        val r = CallResponse.Builder()
        if (block) r.setDisallowCall(true).setRejectCall(true).setSkipNotification(false).setSkipCallLog(false)
        respondToCall(details, r.build())
    }
}
