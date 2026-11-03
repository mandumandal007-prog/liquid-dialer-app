package com.mandu.liquiddialer.ui

import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mandu.liquiddialer.AppState
import com.mandu.liquiddialer.MainActivity
import com.mandu.liquiddialer.data.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CallbacksPage(st: AppState, act: MainActivity, onBack: () -> Unit, onOpenNumber: (String) -> Unit) {
    var visible by remember { mutableIntStateOf(20) }
    val p = LocalLd.current
    val items = remember(st.recents) { PendingCallbacks.from(st.recents) }
    val format = remember { SimpleDateFormat("d MMM, h:mm a", Locale.getDefault()) }
    PageFrame("Wapas call karna",onBack) {
        LdText("Sirf load hui call history se. Baad mein connected call ho toh number yahan se hat jayega. Sirf dial karna kaafi nahi.",12f,color=p.muted,maxLines=5)
        Spacer(Modifier.height(12.dp))
        when {
            !act.has(Manifest.permission.READ_CALL_LOG) -> EmptyState(LdIcons.Phone,"Call history ki permission chahiye","Pending missed calls dikhane ke liye call log access do","Permission do") { act.askPermissions() }
            st.recentsError -> EmptyState(LdIcons.Clock,"Call history load nahi hui","Recents dobara kholkar try karo")
            !st.recentsLoaded -> EmptyState(LdIcons.Clock,"Call history taiyaar ho rahi hai","Thodi der baad dobara dekho")
            items.isEmpty() -> EmptyState(LdIcons.Check,"Abhi pending missed call nahi","Load hui history mein koi unanswered missed call nahi mili")
            else -> {
                LdText("${items.size} numbers · missed calls",12f,FontWeight.SemiBold,color=p.muted,modifier=Modifier.padding(bottom=10.dp))
                items.take(visible).forEach { item ->
                    val r=item.latest
                    val name=st.contacts.firstOrNull { ContactControls.k(it.number)==ContactControls.k(r.number) }?.name?.takeIf {it.isNotBlank()} ?: r.name?.takeIf {it.isNotBlank()} ?: Repo.pretty(r.number)
                    GlassCard(Modifier.padding(bottom=10.dp).testTag("callback_${r.id}")) {
                        LdText(name,16f,FontWeight.Bold,maxLines=2)
                        LdText(Repo.pretty(r.number),12f,color=p.muted,maxLines=2)
                        LdText("${item.missed} missed · ${format.format(Date(r.date))}",12f,color=p.danger,maxLines=2)
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                            PrimaryButton("Call karo",modifier=Modifier.weight(1f).testTag("callback_call_${r.id}")) { act.callUi(r.number) }
                            PrimaryButton("Details",modifier=Modifier.weight(1f).testTag("callback_details_${r.id}")) { onOpenNumber(r.number) }
                        }
                    }
                }
                if(items.size>visible) PrimaryButton("Aur ${minOf(20,items.size-visible)} dikhao") { visible+=20 }
            }
        }
    }
}
