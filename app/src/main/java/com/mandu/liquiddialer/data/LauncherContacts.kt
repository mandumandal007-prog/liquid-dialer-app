package com.mandu.liquiddialer.data
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import com.mandu.liquiddialer.MainActivity
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

/** Icon long-press tasks, distinct from the existing pinned direct-call buttons. */
object LauncherContacts{
 const val ACTION="com.mandu.liquiddialer.LAUNCHER_CONTACT"
 const val PREFIX="launcher_contact_"
 const val NEW=PREFIX+"new"
 data class Target(val name:String,val number:String){fun validate(){require(name.isNotBlank()&&name.length<=40&&name.none{it.isISOControl()});CallLists.number(number);require(number.all{it in '0'..'9'||it in "+-(). "})}}
 data class Config(val enabled:Boolean=false,val targets:List<Target> = emptyList(),val photos:Boolean=false){fun validate(){require(targets.size<=3);targets.forEach{it.validate()};require(targets.map{ContactControls.k(it.number)}.distinct().size==targets.size)}}
 data class Saved(val raw:String,val config:Config)
 data class Published(val requested:Int,val capacity:Int,val enabled:Boolean)
 fun decode(raw:String):Config{if(raw.isEmpty())return Config();require(raw.length<=5000&&'\u0000' !in raw);val tok=JSONTokener(raw);val o=tok.nextValue()as?JSONObject?:error("Invalid shortcut config");require(tok.nextClean()=='\u0000'&&o.get("version")is Int&&o.getInt("version")==1&&o.get("enabled")is Boolean);val a=o.getJSONArray("targets");require(a.length()<=3);val c=Config(o.getBoolean("enabled"),(0 until a.length()).map{val r=a.getJSONObject(it);require(r.get("name")is String&&r.get("number")is String);Target(r.getString("name"),r.getString("number"))},if(o.has("photos")){require(o.get("photos")is Boolean);o.getBoolean("photos")}else false);c.validate();return c}
 fun encode(c:Config):String{c.validate();val a=JSONArray();c.targets.forEach{a.put(JSONObject().put("name",it.name).put("number",it.number))};return JSONObject().put("version",1).put("enabled",c.enabled).put("photos",c.photos).put("targets",a).toString().also{decode(it)}}
 private fun prefs(ctx:Context)=ctx.getSharedPreferences("launcher_contacts",Context.MODE_PRIVATE)
 @Synchronized fun read(ctx:Context):Saved{val raw=prefs(ctx).getString("data","")?:"";return Saved(raw,decode(raw))}
 @Synchronized fun save(ctx:Context,expected:String,c:Config):Saved{val next=encode(c);require(read(ctx).raw==expected){"Saved shortcuts changed. Reload first."};check(prefs(ctx).edit().putString("data",next).commit()){"Settings save failed"};return Saved(next,c)}
 fun id(target:Target)=PREFIX+java.security.MessageDigest.getInstance("SHA-256").digest(ContactControls.k(target.number).toByteArray(Charsets.UTF_8)).take(16).joinToString(""){"%02x".format(it.toInt() and 255)}
 /** Tap resolves only current saved slot; old number extras never drive a call. */
 fun resolve(config:Config,shortcutId:String):Target?{if(!config.enabled)return null;return config.targets.firstOrNull{id(it)==shortcutId}}
 fun intent(ctx:Context,id:String)=Intent(ctx,MainActivity::class.java).setAction(ACTION).putExtra("launcherShortcutId",id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
 fun infos(ctx:Context,c:Config,capacity:Int):List<ShortcutInfo>{c.validate();if(!c.enabled||capacity<=0)return emptyList();val activity=android.content.ComponentName(ctx,MainActivity::class.java)
  val new=ShortcutInfo.Builder(ctx,NEW).setShortLabel("New contact").setLongLabel("New contact").setRank(0).setActivity(activity).setIcon(Icon.createWithResource(ctx,com.mandu.liquiddialer.R.drawable.ic_launcher_foreground)).setIntent(intent(ctx,NEW)).build()
  return listOf(new)+c.targets.take((capacity-1).coerceIn(0,3)).mapIndexed{i,t->ShortcutInfo.Builder(ctx,id(t)).setShortLabel(t.name).setLongLabel(t.name+" · "+t.number).setRank(i+1).setActivity(activity).setIcon(Icon.createWithBitmap(LauncherContactPhotos.icon(ctx,t,c.photos).bitmap)).setIntent(intent(ctx,id(t))).build()}
 }
 @Synchronized fun publish(ctx:Context):Published{
  val c=read(ctx).config;val manager=ctx.getSystemService(ShortcutManager::class.java)?:error("Launcher shortcut service nahi mila")
  require(!manager.isRateLimitingActive){"Android ne abhi shortcut updates roke hain. Baad mein Refresh try karo."}
  val foreign=manager.dynamicShortcuts.filterNot{it.id.startsWith(PREFIX)}
  val capacity=(manager.maxShortcutCountPerActivity-foreign.size-manager.manifestShortcuts.size).coerceIn(0,4)
  val next=infos(ctx,c,capacity);val ids=next.map{it.id}
  val owned=(manager.dynamicShortcuts+manager.pinnedShortcuts).filter{it.id.startsWith(PREFIX)}.map{it.id}.distinct()
  if(ids.isNotEmpty())manager.enableShortcuts(ids)
  check(manager.setDynamicShortcuts(foreign+next)){"Launcher update accept nahi hua"}
  val gone=owned.filter{it !in ids};if(gone.isNotEmpty()){manager.removeDynamicShortcuts(gone);manager.disableShortcuts(gone,"Shortcut turned off or removed in Liquid Dialer settings")}
  return Published(next.size,capacity,c.enabled)
 }
}
