# Regression tests (Robolectric, no emulator)

Run one test class (each Robolectric test is memory heavy; run single tests on small machines):

    ./gradlew testDebugUnitTest --tests '*FlowTest'
    SHOT_DIR=/tmp/shots ./gradlew testDebugUnitTest --tests '*ScreenshotTest'

FlowTest covers: search -> Recents/Contacts query handling, bottom-bar swipes (no call started),
keypad digit taps, live suggestion chip (fills number, never calls), search mic (on-device path, consent gate, unavailable notice),
and simulated 48dp nav-bar inset for the recents list and the dialer sheet.
Inset is simulated through ViewCompat.dispatchApplyWindowInsets; it is not a device. Real speech engines are device-only.
Screenshots go to $SHOT_DIR (default build/shots).
