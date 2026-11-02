package com.mandu.liquiddialer.data
import android.app.Activity
import android.content.Intent
import android.provider.ContactsContract
import android.widget.Toast

/** Opens user-owned contact UI, never writes contacts itself. Always asks compatible handler. */
object ContactApps {
    fun insert(number: String): Intent = Intent(Intent.ACTION_INSERT).apply {
        type = ContactsContract.Contacts.CONTENT_TYPE
        putExtra(ContactsContract.Intents.Insert.PHONE,number)
    }
    fun edit(id: Long,lookup: String): Intent = Intent(Intent.ACTION_EDIT).apply {
        setDataAndType(ContactsContract.Contacts.getLookupUri(id,lookup),ContactsContract.Contacts.CONTENT_ITEM_TYPE)
    }
    fun open(activity: Activity,target: Intent): Boolean = try {
        // No resolveActivity precheck: package visibility must not hide a valid handler.
        activity.startActivity(Intent.createChooser(target,"Contact app chuno"));true
    } catch (_: android.content.ActivityNotFoundException) {
        Toast.makeText(activity,"Compatible contact app nahi mili; app editor istemal karo",Toast.LENGTH_LONG).show();false
    } catch (_: SecurityException) {
        Toast.makeText(activity,"Contact app khul nahi paayi",Toast.LENGTH_LONG).show();false
    }
}
