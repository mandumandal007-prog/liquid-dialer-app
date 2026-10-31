package com.mandu.liquiddialer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mandu.liquiddialer.data.AppLock

/** Full-screen gate jab app lock ON hai. Yahan KOI call/contact/recents content nahi hota. */
@Composable
fun AppLockScreen(onUnlock: () -> Unit) {
    val p = LocalLd.current
    LaunchedEffect(Unit) { if (AppLock.autoPrompt) onUnlock() }
    Box(Modifier.fillMaxSize().background(p.bg), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 32.dp)) {
            LdIcon(LdIcons.Lock, 56.dp, p.accent)
            Spacer(Modifier.height(18.dp))
            LdText("Liquid Dialer lock", 22f, FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            LdText("Fingerprint ya phone ka PIN/Pattern/Password se kholo", 13.5f, color = p.muted, maxLines = 3,
                modifier = Modifier.fillMaxWidth(), letter = 0f)
            Spacer(Modifier.height(26.dp))
            Row(
                Modifier.glass(p, RoundedCornerShape(16.dp), 2.dp)
                    .clickable { onUnlock() }
                    .padding(horizontal = 26.dp, vertical = 13.dp)) {
                LdText("Unlock karo", 15f, FontWeight.SemiBold, p.accent)
            }
        }
    }
}
