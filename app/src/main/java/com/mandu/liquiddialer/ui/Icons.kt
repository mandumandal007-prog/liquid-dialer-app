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
    val Menu: ImageVector by lazy { build("menu", *PARTS.getValue("menu")) }
    val Search: ImageVector by lazy { build("search", *PARTS.getValue("search")) }
    val X: ImageVector by lazy { build("x", *PARTS.getValue("x")) }
    val Chart: ImageVector by lazy { build("chart", *PARTS.getValue("chart")) }
    val History: ImageVector by lazy { build("history", *PARTS.getValue("history")) }
    val Contacts: ImageVector by lazy { build("contacts", *PARTS.getValue("contacts")) }
    val Clock: ImageVector by lazy { build("clock", *PARTS.getValue("clock")) }
    val Users: ImageVector by lazy { build("users", *PARTS.getValue("users")) }
    val Dialpad: ImageVector by lazy { build("dialpad", *PARTS.getValue("dialpad")) }
    // Simple filled handset; separate end-call glyph avoids rotating the call symbol.
    val Phone: ImageVector by lazy { build("phone", *PARTS.getValue("phone")) }
    val CallEnd: ImageVector by lazy { build("call-end", *PARTS.getValue("call-end")) }
    val PhoneOff: ImageVector by lazy { build("phone-off", *PARTS.getValue("phone-off")) }
    val PhoneIn: ImageVector by lazy { build("phone-in", *PARTS.getValue("phone-in")) }
    val PhoneOut: ImageVector by lazy { build("phone-out", *PARTS.getValue("phone-out")) }
    val PhoneMissed: ImageVector by lazy { build("phone-missed", *PARTS.getValue("phone-missed")) }
    val Mic: ImageVector by lazy { build("mic", *PARTS.getValue("mic")) }
    val Volume: ImageVector by lazy { build("volume", *PARTS.getValue("volume")) }
    val SpeakerFill: ImageVector by lazy { build("speaker-fill", *PARTS.getValue("speaker-fill")) }
    val Eye: ImageVector by lazy { build("eye", *PARTS.getValue("eye")) }
    val EyeOff: ImageVector by lazy { build("eye-off", *PARTS.getValue("eye-off")) }
    val Dots: ImageVector by lazy { build("dots", *PARTS.getValue("dots")) }
    val Pause: ImageVector by lazy { build("pause", *PARTS.getValue("pause")) }
    val PersonPlus: ImageVector by lazy { build("person-plus", *PARTS.getValue("person-plus")) }
    val Plus: ImageVector by lazy { build("plus", *PARTS.getValue("plus")) }
    val Sim: ImageVector by lazy { build("sim", *PARTS.getValue("sim")) }
    val Backspace: ImageVector by lazy { build("backspace", *PARTS.getValue("backspace")) }
    val ChevronDown: ImageVector by lazy { build("chevron-down", *PARTS.getValue("chevron-down")) }
    val Chevron: ImageVector by lazy { build("chevron", *PARTS.getValue("chevron")) }
    val ArrowLeft: ImageVector by lazy { build("arrow-left", *PARTS.getValue("arrow-left")) }
    val Check: ImageVector by lazy { build("check", *PARTS.getValue("check")) }
    val Edit: ImageVector by lazy { build("edit", *PARTS.getValue("edit")) }
    val Trash: ImageVector by lazy { build("trash", *PARTS.getValue("trash")) }
    val Rotate: ImageVector by lazy { build("rotate", *PARTS.getValue("rotate")) }
    val Settings: ImageVector by lazy { build("settings", *PARTS.getValue("settings")) }
    val Palette: ImageVector by lazy { build("palette", *PARTS.getValue("palette")) }
    val User: ImageVector by lazy { build("user", *PARTS.getValue("user")) }
    val Star: ImageVector by lazy { build("star", *PARTS.getValue("star")) }
    val StarFill: ImageVector by lazy { build("star-fill", *PARTS.getValue("star-fill")) }
    val Lock: ImageVector by lazy { build("lock", *PARTS.getValue("lock")) }
    val Block: ImageVector by lazy { build("block", *PARTS.getValue("block")) }
    val Message: ImageVector by lazy { build("message", *PARTS.getValue("message")) }
    val Bluetooth: ImageVector by lazy { build("bluetooth", *PARTS.getValue("bluetooth")) }
    val Merge: ImageVector by lazy { build("merge", *PARTS.getValue("merge")) }
    val Swap: ImageVector by lazy { build("swap", *PARTS.getValue("swap")) }
    val Voicemail: ImageVector by lazy { build("voicemail", *PARTS.getValue("voicemail")) }
    val Info: ImageVector by lazy { build("info", *PARTS.getValue("info")) }
    val PhoneSlash: ImageVector by lazy { build("phone-slash", *PARTS.getValue("phone-slash")) }
    val Record: ImageVector by lazy { build("record", *PARTS.getValue("record")) }
    val Play: ImageVector by lazy { build("play", *PARTS.getValue("play")) }
    val Video: ImageVector by lazy { build("video", *PARTS.getValue("video")) }
    val Share: ImageVector by lazy { build("share", *PARTS.getValue("share")) }

    // ---- Icon style packs (user setting, appearance backup ke saath save/restore) ----
    // 0..5 retain old stroke presets; 6/7 add outline/duotone treatment to filled glyphs.
    val ICON_STYLES = listOf("Default", "Patli", "Moti", "Flat", "Flat patli", "Flat moti", "Outline", "Duotone")
    private val STROKE_MULT = floatArrayOf(1f, .65f, 1.5f, 1f, .65f, 1.5f)

    private val PARTS: Map<String, Array<Triple<String, Boolean, Float>>> = mapOf(
        "chart" to arrayOf(Triple("M4 4v16h17M8 15v-4M13 15V7M18 15v-6", false, 1.8f)),
        "history" to arrayOf(Triple("M3.5 11a8.5 8.5 0 1 1 2.3 6.8M3.5 5.5V11H9", false, 1.8f), Triple("M12 7.5V12l3 2", false, 1.8f)),
        "contacts" to arrayOf(Triple("M7 3.5h12a1.5 1.5 0 0 1 1.5 1.5v14a1.5 1.5 0 0 1-1.5 1.5H7a1.5 1.5 0 0 1-1.5-1.5V5A1.5 1.5 0 0 1 7 3.5zM3 7h4M3 12h4M3 17h4", false, 1.8f), Triple("M10.5 9a2.5 2.5 0 1 0 5 0a2.5 2.5 0 1 0-5 0M9 17c.3-2 1.8-3 4-3s3.7 1 4 3", false, 1.8f)),
        "menu" to arrayOf(Triple("M4 7h16M4 12h16M4 17h10", false, 1.8f)),
        "search" to arrayOf(Triple("M4.5,11.0a6.5,6.5 0 1,0 13.0,0a6.5,6.5 0 1,0 -13.0,0z", false, 1.8f), Triple("M20 20l-4.2-4.2", false, 1.8f)),
        "x" to arrayOf(Triple("M6 6l12 12M18 6L6 18", false, 1.8f)),
        "clock" to arrayOf(Triple("M3.5,12.0a8.5,8.5 0 1,0 17.0,0a8.5,8.5 0 1,0 -17.0,0z", false, 1.8f), Triple("M12 7.5V12l3 2", false, 1.8f)),
        "users" to arrayOf(Triple("M5.6,8.0a3.4,3.4 0 1,0 6.8,0a3.4,3.4 0 1,0 -6.8,0z", false, 1.8f), Triple("M2.8 19c.6-3.1 3.1-4.9 6.2-4.9s5.6 1.8 6.2 4.9", false, 1.8f), Triple("M16.5 5.3a3.2 3.2 0 0 1 0 6.1", false, 1.8f), Triple("M18 14.4c2.1.4 3.5 2.1 3.9 4.6", false, 1.8f)),
        "dialpad" to arrayOf(Triple("M4.6,6.0a1.4,1.4 0 1,0 2.8,0a1.4,1.4 0 1,0 -2.8,0z", false, 1.8f), Triple("M10.6,6.0a1.4,1.4 0 1,0 2.8,0a1.4,1.4 0 1,0 -2.8,0z", false, 1.8f), Triple("M16.6,6.0a1.4,1.4 0 1,0 2.8,0a1.4,1.4 0 1,0 -2.8,0z", false, 1.8f), Triple("M4.6,12.0a1.4,1.4 0 1,0 2.8,0a1.4,1.4 0 1,0 -2.8,0z", false, 1.8f), Triple("M10.6,12.0a1.4,1.4 0 1,0 2.8,0a1.4,1.4 0 1,0 -2.8,0z", false, 1.8f), Triple("M16.6,12.0a1.4,1.4 0 1,0 2.8,0a1.4,1.4 0 1,0 -2.8,0z", false, 1.8f), Triple("M4.6,18.0a1.4,1.4 0 1,0 2.8,0a1.4,1.4 0 1,0 -2.8,0z", false, 1.8f), Triple("M10.6,18.0a1.4,1.4 0 1,0 2.8,0a1.4,1.4 0 1,0 -2.8,0z", false, 1.8f), Triple("M16.6,18.0a1.4,1.4 0 1,0 2.8,0a1.4,1.4 0 1,0 -2.8,0z", false, 1.8f)),
        "phone" to arrayOf(Triple("M6.62 10.79a15.05 15.05 0 0 0 6.59 6.59l2.2-2.2a1 1 0 0 1 1.02-.24c1.12.37 2.33.57 3.57.57a1 1 0 0 1 1 1V20a1 1 0 0 1-1 1C10.61 21 3 13.39 3 4a1 1 0 0 1 1-1h3.5a1 1 0 0 1 1 1c0 1.25.2 2.45.57 3.57a1 1 0 0 1-.25 1.02z", true, 0f)),
        "call-end" to arrayOf(Triple("M12 9c-1.6 0-3.15.25-4.6.72v3.1a1 1 0 0 1-.59.91c-.98.49-1.88 1.12-2.68 1.85a1 1 0 0 1-1.38-.03L.27 13.07a1 1 0 0 1 .01-1.42C3.33 8.76 7.45 7 12 7s8.67 1.76 11.72 4.65a1 1 0 0 1 .01 1.42l-2.48 2.48a1 1 0 0 1-1.38.03 11.92 11.92 0 0 0-2.68-1.85 1 1 0 0 1-.59-.91v-3.1A14.96 14.96 0 0 0 12 9z", true, 0f)),
        "phone-off" to arrayOf(Triple("M7.1 3.4c.6-.5 1.5-.4 2 .2l2.4 2.9c.5.6.4 1.5-.2 2l-1.3 1.1a12.4 12.4 0 0 0 6.3 6.3l1.1-1.3c.5-.6 1.4-.7 2-.2l2.9 2.4c.6.5.7 1.4.2 2l-1.4 1.7c-.8.9-2 1.3-3.2 1-5.2-1.4-9.3-5.5-10.7-10.7-.3-1.2.1-2.4 1-3.2z", true, 1.8f), Triple("M3 3l18 18", false, 2.2f)),
        "phone-in" to arrayOf(Triple("M7.1 3.4c.6-.5 1.5-.4 2 .2l2.4 2.9c.5.6.4 1.5-.2 2l-1.3 1.1a12.4 12.4 0 0 0 6.3 6.3l1.1-1.3c.5-.6 1.4-.7 2-.2l2.9 2.4c.6.5.7 1.4.2 2l-1.4 1.7c-.8.9-2 1.3-3.2 1-5.2-1.4-9.3-5.5-10.7-10.7-.3-1.2.1-2.4 1-3.2z", true, 1.8f), Triple("M21 3l-5 5M21 8V3h-5", false, 1.8f)),
        "phone-out" to arrayOf(Triple("M7.1 3.4c.6-.5 1.5-.4 2 .2l2.4 2.9c.5.6.4 1.5-.2 2l-1.3 1.1a12.4 12.4 0 0 0 6.3 6.3l1.1-1.3c.5-.6 1.4-.7 2-.2l2.9 2.4c.6.5.7 1.4.2 2l-1.4 1.7c-.8.9-2 1.3-3.2 1-5.2-1.4-9.3-5.5-10.7-10.7-.3-1.2.1-2.4 1-3.2z", true, 1.8f), Triple("M16 8l5-5M16 3h5v5", false, 1.8f)),
        "phone-missed" to arrayOf(Triple("M7.1 3.4c.6-.5 1.5-.4 2 .2l2.4 2.9c.5.6.4 1.5-.2 2l-1.3 1.1a12.4 12.4 0 0 0 6.3 6.3l1.1-1.3c.5-.6 1.4-.7 2-.2l2.9 2.4c.6.5.7 1.4.2 2l-1.4 1.7c-.8.9-2 1.3-3.2 1-5.2-1.4-9.3-5.5-10.7-10.7-.3-1.2.1-2.4 1-3.2z", true, 1.8f), Triple("M15 3l6 6M21 3l-6 6", false, 1.8f)),
        "mic" to arrayOf(Triple("M12.0,3.0h0.0a3.0,3.0 0 0 1 3.0,3.0v5.0a3.0,3.0 0 0 1 -3.0,3.0h-0.0a3.0,3.0 0 0 1 -3.0,-3.0v-5.0a3.0,3.0 0 0 1 3.0,-3.0z", false, 1.8f), Triple("M5 11a7 7 0 0 0 14 0", false, 1.8f), Triple("M12 18v3", false, 1.8f)),
        "volume" to arrayOf(Triple("M4 10v4h3l4 3.5v-15L7 10z", false, 1.8f), Triple("M15 9a4.5 4.5 0 0 1 0 6", false, 1.8f), Triple("M17.5 6.5a8 8 0 0 1 0 11", false, 1.8f)),
        "speaker-fill" to arrayOf(Triple("M3 9v6h4l5 5V4L7 9H3z", true, 1.8f), Triple("M16.5 12c0-1.77-1.02-3.29-2.5-4.03v8.05c1.48-.73 2.5-2.25 2.5-4.02z", true, 1.8f), Triple("M14 3.23v2.06c2.89.86 5 3.54 5 6.71s-2.11 5.85-5 6.71v2.06c4.01-.91 7-4.49 7-8.77s-2.99-7.86-7-8.77z", true, 1.8f)),
        "eye" to arrayOf(Triple("M2.5 12c2-3.8 5.2-6 9.5-6s7.5 2.2 9.5 6c-2 3.8-5.2 6-9.5 6s-7.5-2.2-9.5-6z", false, 1.8f), Triple("M9.0,12.0a3.0,3.0 0 1,0 6.0,0a3.0,3.0 0 1,0 -6.0,0z", false, 1.8f)),
        "eye-off" to arrayOf(Triple("M4 4l16 16", false, 1.8f), Triple("M9.9 5.6A9.4 9.4 0 0 1 12 5.5c4.3 0 7.5 2.2 9.5 6a13.6 13.6 0 0 1-2.7 3.6M6.2 7.6A12.9 12.9 0 0 0 2.5 11.5c2 3.8 5.2 6 9.5 6 1.3 0 2.5-.24 3.6-.66", false, 1.8f), Triple("M9.9 9.9a3 3 0 0 0 4.2 4.2", false, 1.8f)),
        "dots" to arrayOf(Triple("M12 5.5a1.6 1.6 0 1 0 0-3.2 1.6 1.6 0 0 0 0 3.2zM12 13.6a1.6 1.6 0 1 0 0-3.2 1.6 1.6 0 0 0 0 3.2zM12 21.7a1.6 1.6 0 1 0 0-3.2 1.6 1.6 0 0 0 0 3.2z", true, 1.8f)),
        "pause" to arrayOf(Triple("M9.0,6.0h1.0a1.0,1.0 0 0 1 1.0,1.0v10.0a1.0,1.0 0 0 1 -1.0,1.0h-1.0a1.0,1.0 0 0 1 -1.0,-1.0v-10.0a1.0,1.0 0 0 1 1.0,-1.0z", false, 1.8f), Triple("M14.0,6.0h1.0a1.0,1.0 0 0 1 1.0,1.0v10.0a1.0,1.0 0 0 1 -1.0,1.0h-1.0a1.0,1.0 0 0 1 -1.0,-1.0v-10.0a1.0,1.0 0 0 1 1.0,-1.0z", false, 1.8f)),
        "person-plus" to arrayOf(Triple("M5.5 7.5a3.5 3.5 0 1 0 7 0a3.5 3.5 0 1 0-7 0M2.5 20v-1c0-3 2.5-5 6.5-5 1.5 0 2.8.3 3.8.9M18.5 12v8M14.5 16h8", false, 1.8f)),
        "plus" to arrayOf(Triple("M12 5v14M5 12h14", false, 1.8f)),
        "sim" to arrayOf(Triple("M7 3h7l4 4v14H7z", false, 1.8f), Triple("M10.5,11.0h3.0a1.0,1.0 0 0 1 1.0,1.0v3.0a1.0,1.0 0 0 1 -1.0,1.0h-3.0a1.0,1.0 0 0 1 -1.0,-1.0v-3.0a1.0,1.0 0 0 1 1.0,-1.0z", false, 1.8f)),
        "backspace" to arrayOf(Triple("M9 5h11a1 1 0 0 1 1 1v12a1 1 0 0 1-1 1H9L3 12z", false, 1.8f), Triple("M12 9.5l5 5M17 9.5l-5 5", false, 1.8f)),
        "chevron-down" to arrayOf(Triple("M6 9l6 6 6-6", false, 1.8f)),
        "chevron" to arrayOf(Triple("M9 6l6 6-6 6", false, 1.8f)),
        "arrow-left" to arrayOf(Triple("M19 12H5M11 6l-6 6 6 6", false, 1.8f)),
        "check" to arrayOf(Triple("M5 12.5l4.5 4.5L19 7", false, 1.8f)),
        "edit" to arrayOf(Triple("M4 20h4l10-10-4-4L4 16z", false, 1.8f), Triple("M13.5 6.5l4 4", false, 1.8f)),
        "trash" to arrayOf(Triple("M4 7h16M9 7V4h6v3M6 7l1 13h10l1-13", false, 1.8f)),
        "rotate" to arrayOf(Triple("M3 12a9 9 0 1 0 3-6.7", false, 1.8f), Triple("M3 4v5h5", false, 1.8f)),
        "settings" to arrayOf(Triple("M8.8,12.0a3.2,3.2 0 1,0 6.4,0a3.2,3.2 0 1,0 -6.4,0z", false, 1.8f), Triple("M19.4 13.5a7.9 7.9 0 0 0 0-3l1.9-1.4-2-3.4-2.2.9a7.8 7.8 0 0 0-2.6-1.5L14.2 2h-4l-.3 2.1a7.8 7.8 0 0 0-2.6 1.5l-2.2-.9-2 3.4 1.9 1.4a7.9 7.9 0 0 0 0 3L3 15.9l2 3.4 2.2-.9a7.8 7.8 0 0 0 2.6 1.5l.3 2.1h4l.3-2.1a7.8 7.8 0 0 0 2.6-1.5l2.2.9 2-3.4-1.9-1.4z", false, 1.8f)),
        "palette" to arrayOf(Triple("M12 3a9 9 0 1 0 0 18c1.2 0 2-.9 2-2 0-1.4-1.2-2-1.2-3.2 0-1 .8-1.8 1.8-1.8H17a4 4 0 0 0 4-4c0-3.9-4-7-9-7z", false, 1.8f), Triple("M6.5,11.0a1.0,1.0 0 1,0 2.0,0a1.0,1.0 0 1,0 -2.0,0z", false, 1.8f), Triple("M9.0,7.5a1.0,1.0 0 1,0 2.0,0a1.0,1.0 0 1,0 -2.0,0z", false, 1.8f), Triple("M13.0,7.5a1.0,1.0 0 1,0 2.0,0a1.0,1.0 0 1,0 -2.0,0z", false, 1.8f)),
        "user" to arrayOf(Triple("M8.4,8.0a3.6,3.6 0 1,0 7.2,0a3.6,3.6 0 1,0 -7.2,0z", false, 1.8f), Triple("M4.5 20c.8-3.6 3.9-5.6 7.5-5.6s6.7 2 7.5 5.6", false, 1.8f)),
        "star" to arrayOf(Triple("M12 3.5l2.6 5.3 5.9.9-4.3 4.1 1 5.8-5.2-2.8-5.2 2.8 1-5.8-4.3-4.1 5.9-.9z", false, 1.8f)),
        "star-fill" to arrayOf(Triple("M12 3.5l2.6 5.3 5.9.9-4.3 4.1 1 5.8-5.2-2.8-5.2 2.8 1-5.8-4.3-4.1 5.9-.9z", true, 1.8f)),
        "lock" to arrayOf(Triple("M6 11h12a1.5,1.5 0 0 1 1.5,1.5v6A1.5,1.5 0 0 1 18,20H6a1.5,1.5 0 0 1 -1.5,-1.5v-6A1.5,1.5 0 0 1 6,11z", false, 1.8f), Triple("M8.2 11V7.8a3.8,3.8 0 1 1 7.6,0V11", false, 1.8f), Triple("M12 14.8v1.7", false, 1.8f)),
        "block" to arrayOf(Triple("M3.5,12.0a8.5,8.5 0 1,0 17.0,0a8.5,8.5 0 1,0 -17.0,0z", false, 1.8f), Triple("M6 6l12 12", false, 1.8f)),
        "message" to arrayOf(Triple("M4 5h16v11H9l-5 4z", false, 1.8f)),
        "bluetooth" to arrayOf(Triple("M7 7l10 10-5 4V3l5 4L7 17", false, 1.8f)),
        "merge" to arrayOf(Triple("M7 4v5a5 5 0 0 0 5 5h0a5 5 0 0 1 5 5v1M17 4v5a5 5 0 0 1-5 5", false, 1.8f), Triple("M4 7l3-3 3 3M14 7l3-3 3 3", false, 1.8f)),
        "swap" to arrayOf(Triple("M7 4L3 8l4 4M3 8h14M17 12l4 4-4 4M21 16H7", false, 1.8f)),
        "voicemail" to arrayOf(Triple("M3.0,12.0a3.5,3.5 0 1,0 7.0,0a3.5,3.5 0 1,0 -7.0,0z", false, 1.8f), Triple("M14.0,12.0a3.5,3.5 0 1,0 7.0,0a3.5,3.5 0 1,0 -7.0,0z", false, 1.8f), Triple("M6.5 15.5h11", false, 1.8f)),
        "info" to arrayOf(Triple("M3.5,12.0a8.5,8.5 0 1,0 17.0,0a8.5,8.5 0 1,0 -17.0,0z", false, 1.8f), Triple("M12 11v5M12 8h0", false, 1.8f)),
        "phone-slash" to arrayOf(Triple("M3 3l18 18", false, 1.8f)),
        "record" to arrayOf(Triple("M3.5,12.0a8.5,8.5 0 1,0 17.0,0a8.5,8.5 0 1,0 -17.0,0z", false, 1.8f), Triple("M8.0,12.0a4.0,4.0 0 1,0 8.0,0a4.0,4.0 0 1,0 -8.0,0z", true, 1.8f)),
        "play" to arrayOf(Triple("M8 5.5v13l10-6.5z", true, 1.8f)),
        "video" to arrayOf(Triple("M3.5,7.5h9.0a2.0,2.0 0 0 1 2.0,2.0v5.0a2.0,2.0 0 0 1 -2.0,2.0h-9.0a2.0,2.0 0 0 1 -2.0,-2.0v-5.0a2.0,2.0 0 0 1 2.0,-2.0z", false, 1.8f), Triple("M14.5 10.7l6-3.2v9l-6-3.2", false, 1.8f)),
        "share" to arrayOf(Triple("M12 15V3M7.5 7.5L12 3l4.5 4.5", false, 1.8f), Triple("M5 12v7h14v-7", false, 1.8f))
    )

    private val NAMES: Map<ImageVector, String> by lazy {
        val m = java.util.IdentityHashMap<ImageVector, String>()
        m[History] = "history"
        m[Contacts] = "contacts"
        m[Chart] = "chart"
        m[Menu] = "menu"
        m[Search] = "search"
        m[X] = "x"
        m[Clock] = "clock"
        m[Users] = "users"
        m[Dialpad] = "dialpad"
        m[Phone] = "phone"
        m[CallEnd] = "call-end"
        m[PhoneOff] = "phone-off"
        m[PhoneIn] = "phone-in"
        m[PhoneOut] = "phone-out"
        m[PhoneMissed] = "phone-missed"
        m[Mic] = "mic"
        m[Volume] = "volume"
        m[SpeakerFill] = "speaker-fill"
        m[Eye] = "eye"
        m[EyeOff] = "eye-off"
        m[Dots] = "dots"
        m[Pause] = "pause"
        m[PersonPlus] = "person-plus"
        m[Plus] = "plus"
        m[Sim] = "sim"
        m[Backspace] = "backspace"
        m[ChevronDown] = "chevron-down"
        m[Chevron] = "chevron"
        m[ArrowLeft] = "arrow-left"
        m[Check] = "check"
        m[Edit] = "edit"
        m[Trash] = "trash"
        m[Rotate] = "rotate"
        m[Settings] = "settings"
        m[Palette] = "palette"
        m[User] = "user"
        m[Star] = "star"
        m[StarFill] = "star-fill"
        m[Lock] = "lock"
        m[Block] = "block"
        m[Message] = "message"
        m[Bluetooth] = "bluetooth"
        m[Merge] = "merge"
        m[Swap] = "swap"
        m[Voicemail] = "voicemail"
        m[Info] = "info"
        m[PhoneSlash] = "phone-slash"
        m[Record] = "record"
        m[Play] = "play"
        m[Video] = "video"
        m[Share] = "share"
        m
    }

    private val styleCache = HashMap<Pair<String, Int>, ImageVector>()

    private fun buildStyled(name: String, style: Int): ImageVector {
        val flat = style in 3..5
        val mult = STROKE_MULT.getOrElse(style) { 1f }
        val cap = if (flat) StrokeCap.Butt else StrokeCap.Round
        val join = if (flat) StrokeJoin.Miter else StrokeJoin.Round
        val b = ImageVector.Builder(name + "-s" + style, 24.dp, 24.dp, 24f, 24f)
        for ((d, fill, sw) in PARTS.getValue(name)) {
            val nodes = PathParser().parsePathString(d).toNodes()
            if (fill && style >= 6) b.addPath(nodes,
                fill = if (style == 7) SolidColor(Color.Black) else null,
                fillAlpha = .22f, stroke = SolidColor(Color.Black), strokeLineWidth = 1.25f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            else if (fill) b.addPath(nodes, fill = SolidColor(Color.Black))
            else b.addPath(nodes, stroke = SolidColor(Color.Black), strokeLineWidth = sw * mult, strokeLineCap = cap, strokeLineJoin = join)
        }
        return b.build()
    }

    /** Diye gaye public icon ka styled variant (style 0 ya unknown icon pe original hi). */
    fun styled(icon: ImageVector, style: Int): ImageVector {
        if (style !in 1 until ICON_STYLES.size) return icon
        val name = NAMES[icon] ?: return icon
        return styleCache.getOrPut(name to style) { buildStyled(name, style) }
    }
}
