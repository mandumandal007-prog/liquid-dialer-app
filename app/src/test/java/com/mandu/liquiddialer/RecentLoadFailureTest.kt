package com.mandu.liquiddialer

import android.content.*
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.CallLog
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.Repo
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34])
class RecentLoadFailureTest {
 private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
 private fun provider(load:(Array<out String>?)->Cursor?) {
  ShadowContentResolver.registerProviderInternal(CallLog.AUTHORITY,object:ContentProvider(){
   override fun onCreate()=true
   override fun query(u:Uri,p:Array<out String>?,s:String?,a:Array<out String>?,o:String?)=load(p)
   override fun insert(u:Uri,v:ContentValues?):Uri?=null
   override fun update(u:Uri,v:ContentValues?,s:String?,a:Array<out String>?)=0
   override fun delete(u:Uri,s:String?,a:Array<out String>?)=0
   override fun getType(u:Uri):String?=null
  })
 }
 @Test fun successfulEmptyIsNotFailure(){provider{MatrixCursor(it!!)};assertTrue(Repo.recents(ctx,strict=true).isEmpty())}
 @Test fun nullCursorFailsOnlyStrictCaller(){
  provider{null};assertTrue(Repo.recents(ctx).isEmpty())
  try{Repo.recents(ctx,strict=true);fail("not a successful empty load")}catch(_:IllegalStateException){}
 }
 @Test fun providerExceptionReachesStrictCaller(){
  provider{throw SecurityException("revoked")};assertTrue(Repo.recents(ctx).isEmpty())
  try{Repo.recents(ctx,strict=true);fail("must report error")}catch(_:SecurityException){}
 }
 @Test fun staleFailureCannotOverwriteNewSuccess(){
  val st=AppState()
  publishRecentLoad(st,emptyList(),false,2,2)
  publishRecentLoad(st,emptyList(),true,1,2)
  assertTrue(st.recentsLoaded);assertFalse(st.recentsError)
 }
 @Test fun currentFailureAndRecoveryPublishTogether(){
  val st=AppState()
  publishRecentLoad(st,emptyList(),true,1,1);assertTrue(st.recentsError);assertTrue(st.recentsLoaded)
  publishRecentLoad(st,emptyList(),false,2,2);assertFalse(st.recentsError)
 }
 @Test fun staleSuccessCannotClearCurrentError(){
  val st=AppState()
  publishRecentLoad(st,emptyList(),true,2,2)
  publishRecentLoad(st,emptyList(),false,1,2);assertTrue(st.recentsError)
 }
 @Test fun cancellationNeverBecomesAnEmptySuccess(){
  provider{throw kotlinx.coroutines.CancellationException("cancelled")}
  for(strict in listOf(false,true))try{Repo.recents(ctx,strict=strict);fail("cancellation swallowed")}catch(_:kotlinx.coroutines.CancellationException){}
 }
}
