package com.mandu.liquiddialer
import android.app.Activity
import android.content.Intent
import android.provider.ContactsContract
import com.mandu.liquiddialer.data.ContactApps
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34])
class ContactAppsTest {
 @Test fun insertKeepsExactNumberAndNoSelectedAccountOrPackage(){
  val i=ContactApps.insert("+44 020 2555 9876")
  assertEquals(Intent.ACTION_INSERT,i.action);assertEquals(ContactsContract.Contacts.CONTENT_TYPE,i.type)
  assertEquals("+44 020 2555 9876",i.getStringExtra(ContactsContract.Intents.Insert.PHONE));assertNull(i.component);assertNull(i.`package`)
  assertFalse(i.hasExtra(ContactsContract.Intents.Insert.NAME))
 }
 @Test fun editTargetsLookupIdentityWithNoUnsavedReplacementFields(){
  val i=ContactApps.edit(11,"lookup:a/b")
  assertEquals(Intent.ACTION_EDIT,i.action);assertEquals(ContactsContract.Contacts.getLookupUri(11,"lookup:a/b"),i.data)
  assertEquals(ContactsContract.Contacts.CONTENT_ITEM_TYPE,i.type);assertNull(i.extras)
 }
 @Test fun chooserHandoffDoesNotWriteContact(){
  // Attach context for error feedback, same as an actual activity.
  val activity=org.robolectric.Robolectric.buildActivity(Activity::class.java).setup().get()
  val target=ContactApps.insert("123")
  assertEquals("123",target.getStringExtra(ContactsContract.Intents.Insert.PHONE))
  assertTrue(ContactApps.open(activity,target))
  val launched=org.robolectric.Shadows.shadowOf(activity).nextStartedActivity
  assertEquals(Intent.ACTION_CHOOSER,launched.action)
  activity.finish()
 }
 class MissingActivity:Activity(){override fun startActivity(i:Intent){throw android.content.ActivityNotFoundException()}}
 @Test fun missingHandlerIsSafe(){val a=org.robolectric.Robolectric.buildActivity(MissingActivity::class.java).setup().get();assertFalse(ContactApps.open(a,ContactApps.insert("123")));a.finish()}

}
