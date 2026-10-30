package com.mandu.liquiddialer.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/** Icons copied 1:1 from the Liquid Dialer web design (24x24, stroke 1.8, round caps). */
object LdIcons {
    private fun build(name: String, vararg parts: Triple<String, Boolean, Float>): ImageVector {
        val b = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
        for ((d, fill, sw) in parts) {
            val nodes = PathParser().parsePathString(d).toNodes()
            if (fill) b.addPath(nodes, fill = SolidColor(Color.Black))
            else b.addPath(nodes, stroke = SolidColor(Color.Black), strokeLineWidth = sw, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        }
        return b.build()
    }
    val Menu: ImageVector by lazy { build("menu", Triple("M4 7h16M4 12h16M4 17h10", false, 1.8f)) }
    val Search: ImageVector by lazy { build("search", Triple("M4.5,11.0a6.5,6.5 0 1,0 13.0,0a6.5,6.5 0 1,0 -13.0,0z", false, 1.8f), Triple("M20 20l-4.2-4.2", false, 1.8f)) }
    val X: ImageVector by lazy { build("x", Triple("M6 6l12 12M18 6L6 18", false, 1.8f)) }
    val Clock: ImageVector by lazy { build("clock", Triple("M3.5,12.0a8.5,8.5 0 1,0 17.0,0a8.5,8.5 0 1,0 -17.0,0z", false, 1.8f), Triple("M12 7.5V12l3 2", false, 1.8f)) }
    val Users: ImageVector by lazy { build("users", Triple("M5.6,8.0a3.4,3.4 0 1,0 6.8,0a3.4,3.4 0 1,0 -6.8,0z", false, 1.8f), Triple("M2.8 19c.6-3.1 3.1-4.9 6.2-4.9s5.6 1.8 6.2 4.9", false, 1.8f), Triple("M16.5 5.3a3.2 3.2 0 0 1 0 6.1", false, 1.8f), Triple("M18 14.4c2.1.4 3.5 2.1 3.9 4.6", false, 1.8f)) }
    val Dialpad: ImageVector by lazy { build("dialpad", Triple("M4.6,6.0a1.4,1.4 0 1,0 2.8,0a1.4,1.4 0 1,0 -2.8,0z", false, 1.8f), Triple("M10.6,6.0a1.4,1.4 0 1,0 2.8,0a1.4,1.4 0 1,0 -2.8,0z", false, 1.8f), Triple("M16.6,6.0a1.4,1.4 0 1,0 2.8,0a1.4,1.4 0 1,0 -2.8,0z", false, 1.8f), Triple("M4.6,12.0a1.4,1.4 0 1,0 2.8,0a1.4,1.4 0 1,0 -2.8,0z", false, 1.8f), Triple("M10.6,12.0a1.4,1.4 0 1,0 2.8,0a1.4,1.4 0 1,0 -2.8,0z", false, 1.8f), Triple("M16.6,12.0a1.4,1.4 0 1,0 2.8,0a1.4,1.4 0 1,0 -2.8,0z", false, 1.8f), Triple("M4.6,18.0a1.4,1.4 0 1,0 2.8,0a1.4,1.4 0 1,0 -2.8,0z", false, 1.8f), Triple("M10.6,18.0a1.4,1.4 0 1,0 2.8,0a1.4,1.4 0 1,0 -2.8,0z", false, 1.8f), Triple("M16.6,18.0a1.4,1.4 0 1,0 2.8,0a1.4,1.4 0 1,0 -2.8,0z", false, 1.8f)) }
    val Phone: ImageVector by lazy { build("phone", Triple("M7.1 3.4c.6-.5 1.5-.4 2 .2l2.4 2.9c.5.6.4 1.5-.2 2l-1.3 1.1a12.4 12.4 0 0 0 6.3 6.3l1.1-1.3c.5-.6 1.4-.7 2-.2l2.9 2.4c.6.5.7 1.4.2 2l-1.4 1.7c-.8.9-2 1.3-3.2 1-5.2-1.4-9.3-5.5-10.7-10.7-.3-1.2.1-2.4 1-3.2z", true, 1.8f)) }
    val PhoneOff: ImageVector by lazy { build("phone-off", Triple("M7.1 3.4c.6-.5 1.5-.4 2 .2l2.4 2.9c.5.6.4 1.5-.2 2l-1.3 1.1a12.4 12.4 0 0 0 6.3 6.3l1.1-1.3c.5-.6 1.4-.7 2-.2l2.9 2.4c.6.5.7 1.4.2 2l-1.4 1.7c-.8.9-2 1.3-3.2 1-5.2-1.4-9.3-5.5-10.7-10.7-.3-1.2.1-2.4 1-3.2z", true, 1.8f), Triple("M3 3l18 18", false, 2.2f)) }
    val PhoneIn: ImageVector by lazy { build("phone-in", Triple("M7.1 3.4c.6-.5 1.5-.4 2 .2l2.4 2.9c.5.6.4 1.5-.2 2l-1.3 1.1a12.4 12.4 0 0 0 6.3 6.3l1.1-1.3c.5-.6 1.4-.7 2-.2l2.9 2.4c.6.5.7 1.4.2 2l-1.4 1.7c-.8.9-2 1.3-3.2 1-5.2-1.4-9.3-5.5-10.7-10.7-.3-1.2.1-2.4 1-3.2z", true, 1.8f), Triple("M21 3l-5 5M21 8V3h-5", false, 1.8f)) }
    val PhoneOut: ImageVector by lazy { build("phone-out", Triple("M7.1 3.4c.6-.5 1.5-.4 2 .2l2.4 2.9c.5.6.4 1.5-.2 2l-1.3 1.1a12.4 12.4 0 0 0 6.3 6.3l1.1-1.3c.5-.6 1.4-.7 2-.2l2.9 2.4c.6.5.7 1.4.2 2l-1.4 1.7c-.8.9-2 1.3-3.2 1-5.2-1.4-9.3-5.5-10.7-10.7-.3-1.2.1-2.4 1-3.2z", true, 1.8f), Triple("M16 8l5-5M16 3h5v5", false, 1.8f)) }
    val PhoneMissed: ImageVector by lazy { build("phone-missed", Triple("M7.1 3.4c.6-.5 1.5-.4 2 .2l2.4 2.9c.5.6.4 1.5-.2 2l-1.3 1.1a12.4 12.4 0 0 0 6.3 6.3l1.1-1.3c.5-.6 1.4-.7 2-.2l2.9 2.4c.6.5.7 1.4.2 2l-1.4 1.7c-.8.9-2 1.3-3.2 1-5.2-1.4-9.3-5.5-10.7-10.7-.3-1.2.1-2.4 1-3.2z", true, 1.8f), Triple("M15 3l6 6M21 3l-6 6", false, 1.8f)) }
    val Mic: ImageVector by lazy { build("mic", Triple("M12.0,3.0h0.0a3.0,3.0 0 0 1 3.0,3.0v5.0a3.0,3.0 0 0 1 -3.0,3.0h-0.0a3.0,3.0 0 0 1 -3.0,-3.0v-5.0a3.0,3.0 0 0 1 3.0,-3.0z", false, 1.8f), Triple("M5 11a7 7 0 0 0 14 0", false, 1.8f), Triple("M12 18v3", false, 1.8f)) }
    val Volume: ImageVector by lazy { build("volume", Triple("M4 10v4h3l4 3.5v-15L7 10z", false, 1.8f), Triple("M15 9a4.5 4.5 0 0 1 0 6", false, 1.8f), Triple("M17.5 6.5a8 8 0 0 1 0 11", false, 1.8f)) }
    val SpeakerFill: ImageVector by lazy { build("speaker-fill", Triple("M3 9v6h4l5 5V4L7 9H3z", true, 1.8f), Triple("M16.5 12c0-1.77-1.02-3.29-2.5-4.03v8.05c1.48-.73 2.5-2.25 2.5-4.02z", true, 1.8f), Triple("M14 3.23v2.06c2.89.86 5 3.54 5 6.71s-2.11 5.85-5 6.71v2.06c4.01-.91 7-4.49 7-8.77s-2.99-7.86-7-8.77z", true, 1.8f)) }
    val Eye: ImageVector by lazy { build("eye", Triple("M2.5 12c2-3.8 5.2-6 9.5-6s7.5 2.2 9.5 6c-2 3.8-5.2 6-9.5 6s-7.5-2.2-9.5-6z", false, 1.8f), Triple("M9.0,12.0a3.0,3.0 0 1,0 6.0,0a3.0,3.0 0 1,0 -6.0,0z", false, 1.8f)) }
    val EyeOff: ImageVector by lazy { build("eye-off", Triple("M4 4l16 16", false, 1.8f), Triple("M9.9 5.6A9.4 9.4 0 0 1 12 5.5c4.3 0 7.5 2.2 9.5 6a13.6 13.6 0 0 1-2.7 3.6M6.2 7.6A12.9 12.9 0 0 0 2.5 11.5c2 3.8 5.2 6 9.5 6 1.3 0 2.5-.24 3.6-.66", false, 1.8f), Triple("M9.9 9.9a3 3 0 0 0 4.2 4.2", false, 1.8f)) }
    val Dots: ImageVector by lazy { build("dots", Triple("M12 5.5a1.6 1.6 0 1 0 0-3.2 1.6 1.6 0 0 0 0 3.2zM12 13.6a1.6 1.6 0 1 0 0-3.2 1.6 1.6 0 0 0 0 3.2zM12 21.7a1.6 1.6 0 1 0 0-3.2 1.6 1.6 0 0 0 0 3.2z", true, 1.8f)) }
    val Pause: ImageVector by lazy { build("pause", Triple("M9.0,6.0h1.0a1.0,1.0 0 0 1 1.0,1.0v10.0a1.0,1.0 0 0 1 -1.0,1.0h-1.0a1.0,1.0 0 0 1 -1.0,-1.0v-10.0a1.0,1.0 0 0 1 1.0,-1.0z", false, 1.8f), Triple("M14.0,6.0h1.0a1.0,1.0 0 0 1 1.0,1.0v10.0a1.0,1.0 0 0 1 -1.0,1.0h-1.0a1.0,1.0 0 0 1 -1.0,-1.0v-10.0a1.0,1.0 0 0 1 1.0,-1.0z", false, 1.8f)) }
    val Plus: ImageVector by lazy { build("plus", Triple("M12 5v14M5 12h14", false, 1.8f)) }
    val Sim: ImageVector by lazy { build("sim", Triple("M7 3h7l4 4v14H7z", false, 1.8f), Triple("M10.5,11.0h3.0a1.0,1.0 0 0 1 1.0,1.0v3.0a1.0,1.0 0 0 1 -1.0,1.0h-3.0a1.0,1.0 0 0 1 -1.0,-1.0v-3.0a1.0,1.0 0 0 1 1.0,-1.0z", false, 1.8f)) }
    val Backspace: ImageVector by lazy { build("backspace", Triple("M9 5h11a1 1 0 0 1 1 1v12a1 1 0 0 1-1 1H9L3 12z", false, 1.8f), Triple("M12 9.5l5 5M17 9.5l-5 5", false, 1.8f)) }
    val ChevronDown: ImageVector by lazy { build("chevron-down", Triple("M6 9l6 6 6-6", false, 1.8f)) }
    val Chevron: ImageVector by lazy { build("chevron", Triple("M9 6l6 6-6 6", false, 1.8f)) }
    val ArrowLeft: ImageVector by lazy { build("arrow-left", Triple("M19 12H5M11 6l-6 6 6 6", false, 1.8f)) }
    val Check: ImageVector by lazy { build("check", Triple("M5 12.5l4.5 4.5L19 7", false, 1.8f)) }
    val Edit: ImageVector by lazy { build("edit", Triple("M4 20h4l10-10-4-4L4 16z", false, 1.8f), Triple("M13.5 6.5l4 4", false, 1.8f)) }
    val Trash: ImageVector by lazy { build("trash", Triple("M4 7h16M9 7V4h6v3M6 7l1 13h10l1-13", false, 1.8f)) }
    val Rotate: ImageVector by lazy { build("rotate", Triple("M3 12a9 9 0 1 0 3-6.7", false, 1.8f), Triple("M3 4v5h5", false, 1.8f)) }
    val Settings: ImageVector by lazy { build("settings", Triple("M8.8,12.0a3.2,3.2 0 1,0 6.4,0a3.2,3.2 0 1,0 -6.4,0z", false, 1.8f), Triple("M19.4 13.5a7.9 7.9 0 0 0 0-3l1.9-1.4-2-3.4-2.2.9a7.8 7.8 0 0 0-2.6-1.5L14.2 2h-4l-.3 2.1a7.8 7.8 0 0 0-2.6 1.5l-2.2-.9-2 3.4 1.9 1.4a7.9 7.9 0 0 0 0 3L3 15.9l2 3.4 2.2-.9a7.8 7.8 0 0 0 2.6 1.5l.3 2.1h4l.3-2.1a7.8 7.8 0 0 0 2.6-1.5l2.2.9 2-3.4-1.9-1.4z", false, 1.8f)) }
    val Palette: ImageVector by lazy { build("palette", Triple("M12 3a9 9 0 1 0 0 18c1.2 0 2-.9 2-2 0-1.4-1.2-2-1.2-3.2 0-1 .8-1.8 1.8-1.8H17a4 4 0 0 0 4-4c0-3.9-4-7-9-7z", false, 1.8f), Triple("M6.5,11.0a1.0,1.0 0 1,0 2.0,0a1.0,1.0 0 1,0 -2.0,0z", false, 1.8f), Triple("M9.0,7.5a1.0,1.0 0 1,0 2.0,0a1.0,1.0 0 1,0 -2.0,0z", false, 1.8f), Triple("M13.0,7.5a1.0,1.0 0 1,0 2.0,0a1.0,1.0 0 1,0 -2.0,0z", false, 1.8f)) }
    val User: ImageVector by lazy { build("user", Triple("M8.4,8.0a3.6,3.6 0 1,0 7.2,0a3.6,3.6 0 1,0 -7.2,0z", false, 1.8f), Triple("M4.5 20c.8-3.6 3.9-5.6 7.5-5.6s6.7 2 7.5 5.6", false, 1.8f)) }
    val Star: ImageVector by lazy { build("star", Triple("M12 3.5l2.6 5.3 5.9.9-4.3 4.1 1 5.8-5.2-2.8-5.2 2.8 1-5.8-4.3-4.1 5.9-.9z", false, 1.8f)) }
    val StarFill: ImageVector by lazy { build("star-fill", Triple("M12 3.5l2.6 5.3 5.9.9-4.3 4.1 1 5.8-5.2-2.8-5.2 2.8 1-5.8-4.3-4.1 5.9-.9z", true, 1.8f)) }
    val Block: ImageVector by lazy { build("block", Triple("M3.5,12.0a8.5,8.5 0 1,0 17.0,0a8.5,8.5 0 1,0 -17.0,0z", false, 1.8f), Triple("M6 6l12 12", false, 1.8f)) }
    val Message: ImageVector by lazy { build("message", Triple("M4 5h16v11H9l-5 4z", false, 1.8f)) }
    val Bluetooth: ImageVector by lazy { build("bluetooth", Triple("M7 7l10 10-5 4V3l5 4L7 17", false, 1.8f)) }
    val Merge: ImageVector by lazy { build("merge", Triple("M7 4v5a5 5 0 0 0 5 5h0a5 5 0 0 1 5 5v1M17 4v5a5 5 0 0 1-5 5", false, 1.8f), Triple("M4 7l3-3 3 3M14 7l3-3 3 3", false, 1.8f)) }
    val Swap: ImageVector by lazy { build("swap", Triple("M7 4L3 8l4 4M3 8h14M17 12l4 4-4 4M21 16H7", false, 1.8f)) }
    val Voicemail: ImageVector by lazy { build("voicemail", Triple("M3.0,12.0a3.5,3.5 0 1,0 7.0,0a3.5,3.5 0 1,0 -7.0,0z", false, 1.8f), Triple("M14.0,12.0a3.5,3.5 0 1,0 7.0,0a3.5,3.5 0 1,0 -7.0,0z", false, 1.8f), Triple("M6.5 15.5h11", false, 1.8f)) }
    val Info: ImageVector by lazy { build("info", Triple("M3.5,12.0a8.5,8.5 0 1,0 17.0,0a8.5,8.5 0 1,0 -17.0,0z", false, 1.8f), Triple("M12 11v5M12 8h0", false, 1.8f)) }
    val PhoneSlash: ImageVector by lazy { build("phone-slash", Triple("M3 3l18 18", false, 1.8f)) }
    val Record: ImageVector by lazy { build("record", Triple("M3.5,12.0a8.5,8.5 0 1,0 17.0,0a8.5,8.5 0 1,0 -17.0,0z", false, 1.8f), Triple("M8.0,12.0a4.0,4.0 0 1,0 8.0,0a4.0,4.0 0 1,0 -8.0,0z", true, 1.8f)) }
    val Play: ImageVector by lazy { build("play", Triple("M8 5.5v13l10-6.5z", true, 1.8f)) }
    val Video: ImageVector by lazy { build("video", Triple("M3.5,7.5h9.0a2.0,2.0 0 0 1 2.0,2.0v5.0a2.0,2.0 0 0 1 -2.0,2.0h-9.0a2.0,2.0 0 0 1 -2.0,-2.0v-5.0a2.0,2.0 0 0 1 2.0,-2.0z", false, 1.8f), Triple("M14.5 10.7l6-3.2v9l-6-3.2", false, 1.8f)) }
    val Share: ImageVector by lazy { build("share", Triple("M12 15V3M7.5 7.5L12 3l4.5 4.5", false, 1.8f), Triple("M5 12v7h14v-7", false, 1.8f)) }
}
