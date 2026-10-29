package com.mandu.liquiddialer

import android.net.Uri
import android.telecom.Call

object FakeCall {
    private val unsafe: Any by lazy {
        val c = Class.forName("sun.misc.Unsafe")
        val f = c.getDeclaredField("theUnsafe"); f.isAccessible = true; f.get(null)
    }
    @Suppress("UNCHECKED_CAST")
    private fun <T> allocate(cls: Class<T>): T {
        val m = unsafe.javaClass.getMethod("allocateInstance", Class::class.java)
        return m.invoke(unsafe, cls) as T
    }
    private fun setf(obj: Any, cls: Class<*>, field: String, v: Any?) {
        try { val f = cls.getDeclaredField(field); f.isAccessible = true; f.set(obj, v) } catch (_: Throwable) { }
    }
    fun make(number: String, name: String, state: Int, connectMillis: Long = 0L): Call {
        val call = allocate(Call::class.java)
        val details = allocate(Call.Details::class.java)
        setf(details, Call.Details::class.java, "mHandle", Uri.parse("tel:$number"))
        setf(details, Call.Details::class.java, "mHandlePresentation", 1)
        setf(details, Call.Details::class.java, "mCallerDisplayName", name)
        setf(details, Call.Details::class.java, "mState", state)
        setf(details, Call.Details::class.java, "mConnectTimeMillis", connectMillis)
        setf(details, Call.Details::class.java, "mConnectTimeMillisElapsed", connectMillis)
        setf(details, Call.Details::class.java, "mDisconnectCause", android.telecom.DisconnectCause(android.telecom.DisconnectCause.UNKNOWN))
        setf(details, Call.Details::class.java, "mCallCapabilities", 0)
        setf(details, Call.Details::class.java, "mCallProperties", 0)
        setf(call, Call::class.java, "mDetails", details)
        setf(call, Call::class.java, "mState", state)
        setf(call, Call::class.java, "mChildren", java.util.ArrayList<Call>())
        setf(call, Call::class.java, "mConferenceableCalls", java.util.ArrayList<Call>())
        return call
    }
}
