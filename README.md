# Liquid Dialer

A real Android phone app (default dialer) with the frosted "liquid glass" Liquid Dialer design.
Kotlin + Jetpack Compose, minSdk 26, targetSdk 35.

## Features
- Default phone app (ROLE_DIALER), own InCallService: incoming full-screen call card (swipe up to answer/decline), in-call screen
- Real outgoing calls via TelecomManager, dual-SIM picker + default SIM
- Mute, speaker / Bluetooth route, hold, DTMF keypad, add call, merge / swap, end, proximity sensor
- Decline with SMS quick replies, missed-call notification with Call back
- Recents from the real call log (delete, clear, per-contact history), call stats
- Contacts from the contacts provider: T9 search, favourites, in-app add / edit / delete with photo
- Block numbers (BlockedNumberContract), block unknown / private callers
- Call recording (MIC fallback on Android 10+), recordings list, call notes
- Speed dial, voicemail, call waiting / forwarding codes, ringtone picker, VCF export / import
- Themes: Midnight, Aurora, Sunset, Daylight, Auto + accent colours, blur strength, text size

APK is built by GitHub Actions on every push to main and published as a release.
