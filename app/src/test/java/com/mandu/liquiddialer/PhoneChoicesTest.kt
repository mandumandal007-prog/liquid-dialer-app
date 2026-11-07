package com.mandu.liquiddialer

import android.content.*
import android.database.*
import android.net.Uri
import android.provider.ContactsContract
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34])
class PhoneChoicesTest {
    private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
    private val reads=mutableListOf<Uri>()
    private var rows=listOf<Pair<String?,Int>>("+1 2025550123" to 2,"0012025550123" to 1,"+44 2025550123" to 3,"9876543210" to 2,"+919876543210" to 1,"" to 2,null to 2)
    private var permissionDenied=false
    @Before fun provider() {
        ShadowContentResolver.registerProviderInternal(ContactsContract.AUTHORITY,object:ContentProvider() {
            override fun onCreate()=true
            override fun query(u:Uri,p:Array<out String>?,s:String?,a:Array<out String>?,o:String?):Cursor {
                reads+=u
                assertEquals(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,u)
                assertEquals(listOf("data1","data2"),p!!.toList())
                assertEquals("contact_id=?",s);assertEquals(listOf("11"),a!!.toList())
                if(permissionDenied)throw SecurityException("revoked")
                return MatrixCursor(p).apply {rows.forEach {addRow(arrayOf(it.first,it.second))}}
            }
            override fun insert(u:Uri,v:ContentValues?):Uri?=error("no write")
            override fun update(u:Uri,v:ContentValues?,s:String?,a:Array<out String>?)=error("no write")
            override fun delete(u:Uri,s:String?,a:Array<out String>?)=error("no write")
            override fun getType(u:Uri):String?=null
        })
    }
    @Test fun onePhoneQueryPreservesOrderTypesAndFullInternationalIdentity() {
        assertEquals(listOf("+1 2025550123" to 2,"+44 2025550123" to 3,"9876543210" to 2),Repo.phoneChoices(ctx,11))
        assertEquals(1,reads.size)
    }
    @Test fun nextReadSeesEditsAndDeletionRatherThanReusingSnapshot() {
        Repo.phoneChoices(ctx,11);rows=listOf("+442025550999" to 1)
        assertEquals(rows,Repo.phoneChoices(ctx,11));rows=emptyList();assertTrue(Repo.phoneChoices(ctx,11).isEmpty())
        assertEquals(3,reads.size)
    }
    @Test fun permissionErrorPropagatesToChooserFailureHandling() {
        permissionDenied=true
        try{Repo.phoneChoices(ctx,11);fail("must not show stale choices")}catch(_:SecurityException){}
        assertEquals(1,reads.size)
    }
}
