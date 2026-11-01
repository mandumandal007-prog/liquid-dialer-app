package com.mandu.liquiddialer
import android.content.*
import android.database.*
import android.net.Uri
import android.provider.*
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
class RecentsNameLoadTest {
    private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
    private val numbers=listOf("+12025559876","+12025559876","+442025559876")
    private var lookups=0
    private var saved:String?=null
    private open class Provider:ContentProvider() {
        override fun onCreate()=true
        override fun query(u:Uri,p:Array<out String>?,s:String?,a:Array<out String>?,o:String?):Cursor?=null
        override fun insert(u:Uri,v:ContentValues?):Uri?=null
        override fun delete(u:Uri,s:String?,a:Array<out String>?)=0
        override fun update(u:Uri,v:ContentValues?,s:String?,a:Array<out String>?)=0
        override fun getType(u:Uri):String?=null
    }
    @Before fun seed() {
        ShadowContentResolver.registerProviderInternal(CallLog.AUTHORITY,object:Provider(){
            override fun query(u:Uri,p:Array<out String>?,s:String?,a:Array<out String>?,o:String?):Cursor = MatrixCursor(p).apply {
                numbers.forEachIndexed { i,n -> addRow(arrayOf(i.toLong(),n,"row-$i",CallLog.Calls.INCOMING_TYPE,1000L-i,60L,null,null)) }
            }
        })
        ShadowContentResolver.registerProviderInternal(ContactsContract.AUTHORITY,object:Provider(){
            override fun query(u:Uri,p:Array<out String>?,s:String?,a:Array<out String>?,o:String?):Cursor? {
                if(!u.toString().contains("phone_lookup"))return null
                lookups++
                return MatrixCursor(p).apply { saved?.let { addRow(arrayOf(it,null)) } }
            }
        })
        ReceivedNames.learn(ctx,numbers[0],"US network",1,1)
        ReceivedNames.learn(ctx,numbers[2],"UK network",1,1)
    }
    @After fun clean(){ctx.getSharedPreferences("received_caller_names",0).edit().clear().apply()}
    @Test fun repeatedRowsResolveOncePerNumberAndForeignSuffixesStaySeparate() {
        val rows=Repo.recents(ctx)
        assertEquals(listOf("US network","US network","UK network"),rows.map { it.name })
        assertEquals(2,lookups)
        assertEquals(numbers,rows.map { it.number })
    }
    @Test fun nextLoadSeesContactSaveAndConsistentlyUsesSavedNameForDuplicateRows() {
        Repo.recents(ctx);lookups=0;saved="Saved by me"
        assertEquals(listOf("Saved by me","Saved by me","Saved by me"),Repo.recents(ctx).map { it.name })
        assertEquals(2,lookups)
        assertNull(ReceivedNames.get(ctx,numbers[0]))
    }
    @Test fun nextLoadSeesDismissAndPreservesEachRowsOwnCachedFallback() {
        Repo.recents(ctx);ReceivedNames.dismiss(ctx,numbers[0]);lookups=0
        assertEquals(listOf("row-0","row-1","UK network"),Repo.recents(ctx).map { it.name })
        assertEquals(1,lookups)
    }
    @Test fun requestedLimitAndOrderRemainUnchanged() {
        assertEquals(listOf(0L,1L),Repo.recents(ctx,2).map { it.id })
        assertEquals(1,lookups)
    }
}
