package com.mandu.liquiddialer
import android.content.*
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
class ContactEditPreserveTest {
 @Test fun savingKnownEditorFieldsKeepsExtraEmailsAndDoesNotDeleteUnrelatedFields(){
  val ctx=ApplicationProvider.getApplicationContext<LdApp>();val ops=mutableListOf<ContentProviderOperation>()
  val provider=object:ContentProvider(){
   override fun onCreate()=true
   override fun query(u:Uri,p:Array<out String>?,s:String?,a:Array<out String>?,o:String?):android.database.Cursor?=null
   override fun insert(u:Uri,v:ContentValues?):Uri?=null
   override fun delete(u:Uri,s:String?,a:Array<out String>?)=0
   override fun update(u:Uri,v:ContentValues?,s:String?,a:Array<out String>?)=0
   override fun getType(u:Uri):String?=null
   override fun applyBatch(operations:ArrayList<ContentProviderOperation>):Array<ContentProviderResult>{ops.addAll(operations);return operations.map {ContentProviderResult(1)}.toTypedArray()}
  }
  ShadowContentResolver.registerProviderInternal(ContactsContract.AUTHORITY,provider)
  val detail=Repo.ContactDetail(11,1101,"lookup","Old",listOf("123" to 2),listOf("one@test.invalid","two@test.invalid"),false,null)
  assertTrue(Repo.saveContact(ctx,detail,"New",listOf("456" to 2),"edited@test.invalid",null,true,detail.emails.drop(1)))
  val text=ops.joinToString()
  assertTrue(text.contains("edited@test.invalid"));assertTrue(text.contains("two@test.invalid"))
  assertFalse(text.contains("vnd.android.cursor.item/postal-address"));assertFalse(text.contains("vnd.android.cursor.item/contact_event"));assertFalse(text.contains("vnd.android.cursor.item/photo"))
 }
}
