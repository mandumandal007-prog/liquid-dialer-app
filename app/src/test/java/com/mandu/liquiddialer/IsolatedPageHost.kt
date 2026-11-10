package com.mandu.liquiddialer
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView

/** Replace the activity scene with a fresh host, not the still-recomposing app host. */
fun MainActivity.mountTestPage(content:@Composable ()->Unit){
 val host=ComposeView(this)
 host.setContent(content)
 setContentView(host)
}
